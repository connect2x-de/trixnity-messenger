config.resolve.fallback = {
    crypto: false,
    fs: false,
    path: false,
    stream: false,
    buffer: false,
    os: false,
    url: false,
};

// Minification
if (config.mode === "production") {
    const TerserPlugin = require("terser-webpack-plugin");
    config.optimization = {
        minimize: true,
        minimizer: [
            new TerserPlugin({
                extractComments: true,
                terserOptions: {
                    compress: {
                        collapse_vars: false, // > 5 min
                        reduce_vars: false, // 30 secs
                    },
                    mangle: true,
                },
            }),
        ],
    };
} else {
    config.optimization = {
        minimize: false,
        minimizer: [],
    };
}
