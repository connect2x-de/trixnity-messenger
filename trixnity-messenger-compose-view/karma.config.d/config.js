const path = require("node:path");

config.browserConsoleLogOptions.level = "debug"

const basePath = config.basePath;
// According to https://kotlinlang.org/docs/js-project-setup.html#karma-configuration all files ending in `.js` are
// from the `karma.config.d` directory are merged into a single configuration. This configuration however does not
// reside in the subprojects build directory, but instead in the main one. The resulting path of the merged config is
// then `build/js/packages/trixnity-messenger-root-trixnity-messenger-compose-view-web-test/karma.conf.js`.
const rootPath = path.resolve(basePath, "..", "..", "..", "..");
const configPath = path.resolve(rootPath, "trixnity-messenger-compose-view", "karma.config.d");

const debug = message => console.error(`[karma-config] ${message}`);

debug(basePath)
debug(rootPath)
debug(configPath)

// TODO: We need a custom context to import `skiko.js` like in trixnity-messenger-compose-app/src/webMain/resources/index.html
// https://youtrack.jetbrains.com/issue/CMP-4906/1nMakeRasterN32Premul-is-not-defined-when-running-runComposeUiTest-with-js
config.customContextFile = path.resolve(configPath, "compose_context.html");
