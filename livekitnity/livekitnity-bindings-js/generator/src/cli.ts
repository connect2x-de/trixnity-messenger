import assert from 'node:assert';
import { mkdirSync, readdirSync, unlinkSync, writeFileSync } from 'node:fs';
import { mkdir, readdir, unlink, writeFile } from 'node:fs/promises';
import path, { resolve } from 'node:path';
import { exit } from 'node:process';
import {
  Project,
  Type,
  Signature,
  Node,
  ts,
  ScriptTarget,
  ModuleKind,
  ModuleResolutionKind,
} from 'ts-morph';

const project = new Project({
  compilerOptions: {
    strict: true,
    target: ScriptTarget.ESNext,
    module: ModuleKind.ESNext,
    moduleResolution: ModuleResolutionKind.Bundler,
    noEmit: true,
  },
});

const eventsSourceFile = project.addSourceFileAtPath('livekit.d.ts');

const diagnostics = project.getPreEmitDiagnostics();
assert.equal(diagnostics.length, 0, project.formatDiagnosticsWithColorAndContext(diagnostics));

const at = eventsSourceFile;
const field = (t: Type, n: string) => t.getProperty(n)?.getTypeAtLocation(at);
const literal = (t: Type) => t.getLiteralValue();
const entries = (name: string) =>
  at
    .getTypeAliasOrThrow(name)
    .getType()
    .getProperties()
    .map((s) => ({ name: s.getName(), spec: s.getTypeAtLocation(at) }));
const bindings = entries('Bindings').map((e) => ({
  ...e,
  source: field(e.spec, 'source')!,
  kind: literal(field(e.spec, 'kind')!),
}));
const ids = new Map<object, number>();
const id = (o: object) => {
  if (!ids.has(o)) ids.set(o, ids.size);
  return ids.get(o)!;
};
const key = (t: Type) =>
  t.isUnion()
    ? t
        .getUnionTypes()
        .map((x) => id(x.compilerType))
        .sort((a, b) => a - b)
        .join(',')
    : String(id(t.compilerType));
const exact = new Map(bindings.map((e) => [key(e.source), e.name]));
assert.equal(exact.size, bindings.length, 'Duplicate source type in Bindings');

const symbols = new Map(
  bindings
    .filter((e) => e.source.getSymbol())
    .map((e) => [e.source.getSymbol()!.compilerSymbol, e.name]),
);
const natives = entries('Platform').map((e) => ({
  source: field(e.spec, 'source')!,
  kotlin: String(literal(field(e.spec, 'kotlin')!)),
}));
const nativeSymbols = new Map(
  natives
    .filter((e) => e.source.getSymbol())
    .map((e) => [e.source.getSymbol()!.compilerSymbol, e.kotlin]),
);
const record = String(
  literal(field(entries('Platform').find((e) => e.name === 'Record')!.spec, 'kotlin')!),
);

const header = `@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress(
    // "NESTED_CLASS_IN_EXTERNAL_INTERFACE",
    "RemoveRedundantBackticks",
    "RemoveExplicitTypeArguments",
    "RedundantUnitReturnType",
    "UnnecessaryOptInAnnotation",
    // "unused",
)

package livekit

import kotlin.js.*

`;
const inventory = createInventory(header);

const quote = (s: string) => JSON.stringify(s).replaceAll('$', '\\$');
const ident = (s: string) => '`' + s + '`';
const title = (s: string) =>
  s.replace(/(^|[^a-zA-Z0-9]+)([a-zA-Z0-9])/g, (_, __, c) => c.toUpperCase());
const parts = (t: Type) => (t.isUnion() ? t.getUnionTypes() : [t]);

function nestedKotlin(t: Type, owner: string): string {
  const result = kotlin(t, owner);
  if (result == 'Unit') return 'Nothing?';
  if (result == 'String') return 'JsString';
  if (result == 'Boolean') return 'JsBoolean';
  return result;
}

function kotlin(t: Type, owner: string): string {
  if (t.isVoid()) return 'Unit';
  const nonnull = t.getNonNullableType();
  if (nonnull.compilerType !== t.compilerType) return kotlin(nonnull, owner) + '?';
  const registered = exact.get(key(t));
  if (registered) return registered;
  // TODO: should we handle native better?
  // assert(!t.isAny() && !t.isUnknown() && !t.isNever(), `${owner}: unsupported ${t.getText(at)}`);
  assert(!t.isUnknown() && !t.isNever(), `${owner}: unsupported ${t.getText(at)}`);
  if (t.isEnumLiteral()) {
    const name = exact.get(key(t.getBaseTypeOfLiteralType()));
    assert(name, `${owner}: missing enum binding ${t.getText(at)}`);
    return name;
  }
  assert(!t.isEnum(), `${owner}: missing enum binding ${t.getText(at)}`);
  if (t.isString()) return 'String';
  if (t.isNumber()) return 'Double';
  if (t.isBoolean() || t.isBooleanLiteral()) return 'Boolean';
  if (t.isAny()) return 'JsAny';
  if (t.isTypeParameter() && t.getText() === 'this') return owner;
  const symbol = t.getSymbol()?.compilerSymbol;
  if (symbol && symbols.has(symbol)) return symbols.get(symbol)!;
  const index = t.getStringIndexType();
  if (index && ['Record', 'Readonly'].includes(t.getAliasSymbol()?.getName() ?? ''))
    return record.replace('{0}', nestedKotlin(index, owner));
  if (symbol && nativeSymbols.has(symbol)) {
    return nativeSymbols
      .get(symbol)!
      .replace(/\{(\d+)\}/g, (_, n) => nestedKotlin(t.getTypeArguments()[Number(n)], owner));
  }
  const calls = t.getCallSignatures();
  if (calls.length) {
    assert.equal(calls.length, 1, `${owner}: overloaded function type`);

    const sig = calls[0];
    const params = sig.getParameters().map((p) => kotlin(p.getTypeAtLocation(at), owner));

    return `(${params.join(', ')}) -> ${kotlin(sig.getReturnType(), owner)}`;
  }
  throw new Error(`${owner}: missing binding for ${t.getText(at)}`);
}

function parameters(sig: Signature, owner: string, defaults: boolean, external: boolean) {
  assert.equal(sig.getTypeParameters().length, 0, `${owner}: generic signature`);
  return sig.getParameters().map((s) => {
    assert(
      !s.getDeclarations().some((d) => Node.isParameterDeclaration(d) && d.isRestParameter()),
      `${owner}: rest parameter`,
    );
    const name = ident(s.getName());
    const type = kotlin(s.getTypeAtLocation(at), owner);
    const optional = s
      .getDeclarations()
      .some((d) => Node.isParameterDeclaration(d) && d.isOptional());
    return {
      name,
      type,
      declaration: `${name}: ${type}${defaults && optional ? (external ? ' = definedExternally' : ' = undefined()') : ''}`,
    };
  });
}
function ancestorMembers(spec: Type): Set<string> {
  const parent = field(spec, 'parent');
  return parent
    ? new Set([
        ...ancestorMembers(parent),
        ...field(parent, 'members')!
          .getProperties()
          .map((s) => s.getName()),
      ])
    : new Set();
}
function properties(spec: Type, owner: string) {
  const inherited = ancestorMembers(spec);
  const mutable = new Set(parts(field(spec, 'mutable')!).map(literal));
  return field(spec, 'members')!
    .getProperties()
    .flatMap((s) => {
      const type = s.getTypeAtLocation(at),
        name = ident(s.getName());
      const override = inherited.has(s.getName()) ? 'override ' : '';
      const calls = type.getCallSignatures();
      if (calls.length)
        return calls.map(
          (sig) =>
            `    public ${override}fun ${name}(${parameters(sig, owner, !override, true)
              .map((p) => p.declaration)
              .join(', ')}): ${kotlin(sig.getReturnType(), owner)}`,
        );
      return [
        `    public ${override}${mutable.has(s.getName()) ? 'var' : 'val'} ${name}: ${kotlin(type, owner)}`,
      ];
    });
}
function objectFactory(spec: Type, name: string) {
  const fields = field(spec, 'members')!
    .getProperties()
    .map((s) => ({
      name: s.getName(),
      type: kotlin(s.getTypeAtLocation(at), name),
      optional: s.isOptional(),
    }));
  const params = fields
    .map((p) => `${ident(p.name)}: ${p.type}${p.optional ? ' = undefined()' : ''}`)
    .join(', ');
  const fieldStmts = fields
    .map((p) => `        field(${quote(p.name)}, ${ident(p.name)}),`)
    .join('\n');

  return `public external interface Mutable${name} : JsAny {
${fields.map((f) => `    public var \`${f.name}\`: ${f.type}`).join('\n')}
}

public fun ${name}(builder: Mutable${name}.() -> Unit): ${name} {
    return unsafeCast<${name}>(createObject<Mutable${name}>().apply(builder))
}`;
}
// The SDK has named exports; js() avoids a redundant external class.
function classFactory(spec: Type, name: string) {
  const signatures = field(spec, 'constructors')!.getConstructSignatures();
  assert.equal(signatures.length, 1, `${name}: expected one constructor`);
  const params = parameters(signatures[0], name, true, false);
  const args = signatures[0]
    .getParameters()
    .map((p) => p.getName())
    .join(', ');
  return `@JsModule("livekit-client")
private external object ${name}Module { val ${ident(name)}: Constructor<${name}> }

public fun ${name}(${params.map((p) => p.declaration).join(', ')}): ${name} {
    return construct(${name}Module.${ident(name)}, ${args})
}`;
}
const maxArity = Math.max(
  ...bindings.flatMap(
    (e) =>
      field(e.spec, 'events')
        ?.getProperties()
        .map((s) => s.getTypeAtLocation(at).getCallSignatures()[0].getParameters().length) ?? [],
  ),
);
function eventObject(spec: Type, owner: string) {
  const events = field(spec, 'events')!.getProperties();
  if (!events.length) return '';
  const lines = events.map((s) => {
    const calls = s.getTypeAtLocation(at).getCallSignatures();
    assert.equal(calls.length, 1);
    const ps = parameters(calls[0], owner, false, true),
      n = ps.length;
    assert(calls[0].getReturnType().isVoid());
    const args = n
      ? '<' +
        ps
          .map((p) => {
            if (p.type === 'String') return 'JsString';
            if (p.type === 'String?') return 'JsString?';
            if (p.type === 'Boolean') return 'JsBoolean';
            if (p.type === 'Boolean?') return 'JsBoolean?';

            return p.type;
          })
          .join(', ') +
        '>'
      : '';
    return `    public val ${title(s.getName())}: EventCallback${n}${args} = eventCallback${n}(${quote(s.getName())})`;
  });
  return `public object ${owner}Event {\n${lines.join('\n')}\n}`;
}
function literals(cases: Type[], name: string) {
  const values = cases
    .map((t) => {
      const value = literal(t);
      assert(value !== undefined, `${name}: nonliteral enum case`);
      const caseName = t.isEnumLiteral() ? t.getSymbol()!.getName() : title(String(value));
      return `    public val ${ident(caseName)}: ${name} get() { return unsafeCast<${name}>(${typeof value === 'string' ? quote(value) : value + '.0'}) }`;
    })
    .join('\n');

  return `public object ${name}s {\n${values}\n}`;
}

const callbackTypes: string[] = [],
  subscriptions: string[] = [];
for (let n = 0; n <= maxArity; n++) {
  const ts = Array.from({ length: n }, (_, i) => `T${i}`);
  const genericDecl = n ? '<' + ts.map((t) => `${t} : JsAny?`).join(', ') + '>' : '';
  const generic = n ? '<' + ts.join(', ') + '>' : '';
  if (n == 0) {
    callbackTypes.push(
      `public external interface EventCallback${n}${genericDecl} : JsAny\ninternal fun ${genericDecl} eventCallback${n}(name: String): EventCallback${n}${generic} { return unsafeCast<EventCallback${n}${generic}>(name) }`,
    );
  } else {
    callbackTypes.push(
      `public external interface EventCallback${n}${genericDecl} : EventCallback0\ninternal fun ${genericDecl} eventCallback${n}(name: String): EventCallback${n}${generic} { return unsafeCast<EventCallback${n}${generic}>(name) }`,
    );
  }
  for (const method of ['on', 'off'])
    subscriptions.push(
      `    public fun ${genericDecl} ${method}(event: EventCallback${n}${generic}, listener: (${ts.join(', ')}) -> Unit)`,
    );
}
for (const { name, spec, source, kind } of bindings) {
  switch (kind) {
    case 'enum':
      inventory.emit(
        name,
        `public external interface ${name} : JsAny\n\n${literals(parts(source), name)}`,
      );
      break;
    case 'union': {
      const alternatives = parts(source),
        primitiveCases: Type[] = alternatives.filter(
          (t) => t.isStringLiteral() || t.isNumberLiteral(),
        );
      const types = [
        ...new Set(
          alternatives.filter((t) => !primitiveCases.includes(t)).map((t) => kotlin(t, name)),
        ),
      ];
      inventory.emit(
        name,
        `public external interface ${name} : JsAny\n\n` +
          types
            .map(
              (t) =>
                `public fun ${name}(value: ${t}): ${name} { return unsafeCast<${name}>(value) }`,
            )
            .join('\n') +
          '\n' +
          (primitiveCases.length ? literals(primitiveCases, name) : ''),
      );
      break;
    }
    case 'class':
    case 'interface': {
      const parent = field(spec, 'parent');
      const base = parent
        ? exact.get(key(field(parent, 'source')!))!
        : kind === 'class'
          ? 'EventEmitter'
          : 'JsAny';
      assert(base, `${name}: missing parent`);
      let body = `public external interface ${name} : ${base} {\n${properties(spec, name).join('\n')}\n}`;
      if (field(spec, 'objectConstructor')?.getText() === 'true')
        body += '\n\n' + objectFactory(spec, name);
      if (field(spec, 'constructors')) body += '\n\n' + classFactory(spec, name);
      if (kind === 'class') body += '\n\n' + eventObject(spec, name);
      inventory.emit(name, body);
      break;
    }
    default:
      throw new Error(`Unknown kind ${kind}`);
  }
}
inventory.emit(
  'Runtime',
  `internal fun <T: JsAny?> undefined(): T = js("undefined")\n\n${callbackTypes.join('\n\n')}\n\npublic external interface EventEmitter : JsAny {\n${subscriptions.join('\n')}\n}

internal fun <T: JsAny> construct(constructor: Constructor<T>, options: JsAny?) : T = js("""new constructor(options)""")

internal external interface Constructor<T : JsAny> : JsAny

internal fun <T: JsAny> unsafeCast(value: Boolean): T = js("""value""")
internal fun <T: JsAny> unsafeCast(value: String): T = js("""value""")
internal fun <T: JsAny> unsafeCast(value: Double): T = js("""value""")
internal fun <T: JsAny> unsafeCast(value: JsAny): T = js("""value""")

internal fun <T: JsAny> createObject(): T = js("""({})""")

public external interface Date : JsAny
public external interface JsMap<K: JsAny?, V: JsAny?> : JsAny
public external interface JsRecord<V : JsAny?> : JsAny

public external interface CryptoKey : JsAny

public external interface JsError : JsAny`,
);

writeBindingsToDirectory('../src/webMain/kotlin/livekit', inventory.files);

interface Inventory {
  readonly files: ReadonlyMap<string, string>;
  emit(name: string, body: string): void;
}

function createInventory(header: string): Inventory {
  const files = new Map<string, string>();

  return {
    get files() {
      return files;
    },
    emit(name: string, body: string) {
      assert(!files.has(name), `Duplicate file ${name}`);
      files.set(name, header + body + '\n');
    },
  };
}

async function writeBindingsToDirectory(directory: string, files: ReadonlyMap<string, string>) {
  await mkdir(directory, { recursive: true });
  await clearKtFiles(directory);
  await writeKtFiles(directory, files);

  console.log(`Wrote ${files.size} Kotlin files to ${directory}`);
}

async function clearKtFiles(directory: string) {
  const existingFiles = await readdir(directory);
  const pathsToRemove = existingFiles
    .values()
    .filter((fileName) => fileName.endsWith('.kt'))
    .map((fileName) => path.join(directory, fileName));

  return Promise.all(pathsToRemove.map(unlink));
}

async function writeKtFiles(directory: string, files: ReadonlyMap<string, string>) {
  async function write([baseName, content]: [string, string]) {
    return writeFile(path.join(directory, `${baseName}.kt`), content);
  }
  return Promise.all(files.entries().map(write));
}
