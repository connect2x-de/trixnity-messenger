if (!!config.client) {
    config.client.mocha = config.client.mocha || {}
    config.client.mocha.timeout = 300000
}
config.browserNoActivityTimeout = 300000
