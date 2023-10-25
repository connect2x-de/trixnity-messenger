// swift-tools-version:5.3
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://gitlab.com/api/v4/projects/47538655/packages/maven/de/connect2x/trixnity-messenger-kmmbridge/1.0.10-LOCAL/trixnity-messenger-kmmbridge-1.0.10-LOCAL.zip"
let remoteKotlinChecksum = "f10770d96f7afaa842a9bd0b9008a46cc6c024b2ef93408ca74b142fb9ca6af9"
let packageName = "trixnity-messenger"
// END KMMBRIDGE BLOCK

let package = Package(
    name: packageName,
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: packageName,
            targets: [packageName]
        ),
    ],
    targets: [
        .binaryTarget(
            name: packageName,
            url: remoteKotlinUrl,
            checksum: remoteKotlinChecksum
        )
        ,
    ]
)