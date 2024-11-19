const path = require("node:path");

config.browserConsoleLogOptions.level = "debug"

const basePath = config.basePath;
const rootPath = path.resolve(basePath, "..", "..", "..", "..");
const configPath = path.resolve(rootPath, "trixnity-messenger-compose-view", "karma.config.d");

const debug = message => console.error(`[karma-config] ${message}`);

debug(basePath)
debug(rootPath)
debug(configPath)

// TODO: We need a custom context to import `skiko.js` like in trixnity-messenger-compose-app/src/webMain/resources/index.html
// https://youtrack.jetbrains.com/issue/CMP-4906/1nMakeRasterN32Premul-is-not-defined-when-running-runComposeUiTest-with-js
config.customContextFile = path.resolve(configPath, "compose_context.html");
