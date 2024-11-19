const path = require("node:path");

config.browserConsoleLogOptions.level = "debug"

const basePath = config.basePath;
const rootPath = path.resolve(basePath, "..", "..", "..", "..");
const configPath = path.resolve(rootPath, "trixnity-messenger-compose-view", "karma.config.d");

const debug = message => console.error(`[karma-config] ${message}`);

debug(basePath)
debug(rootPath)
debug(configPath)

config.customContextFile = path.resolve(configPath, "compose_context.html");
