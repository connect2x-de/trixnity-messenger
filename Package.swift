// swift-tools-version:5.3
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://gitlab.com/api/v4/projects/47538655/packages/maven/de/connect2x/trixnity-messenger-kmmbridge/1.0.8-LOCAL/trixnity-messenger-kmmbridge-1.0.8-LOCAL.zip"
let remoteKotlinChecksum = "2d68590fdc29e3c1f30d80bbbefe5aaf52a1db13a7276e7bd8ba211056451229"
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