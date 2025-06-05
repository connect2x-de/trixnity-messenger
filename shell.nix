{ pkgs ? import <nixpkgs> { config.allowUnfree = true; config.android_sdk.accept_license = true; } }:
  let
    unstableTarball = fetchTarball "https://github.com/NixOS/nixpkgs/archive/nixos-unstable.tar.gz";
    unstable = import unstableTarball { config.allowUnfree = true; };
    java = pkgs.zulu17;
    gradle = pkgs.gradle.override { inherit java; };
    kotlin = pkgs.kotlin.override { jre = java; };
    intellij = unstable.jetbrains.idea-ultimate;
    androidSdk = pkgs.androidenv.androidPkgs_9_0.androidsdk;
  in
  pkgs.mkShell {
    nativeBuildInputs = with pkgs; [
      java
      gradle
      kotlin
      intellij
      libGL
      xorg.libX11
      fontconfig
      androidSdk
      glibc
    ];
    GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdk}/libexec/android-sdk/build-tools/28.0.3/aapt2";
    shellHook = ''
      export BASE_DIR=$(pwd)
      mkdir -p $BASE_DIR/.share
      
      if [ -L "$BASE_DIR/.share/java" ]; then
        unlink "$BASE_DIR/.share/java"
      fi
      ln -sf ${java}/lib/openjdk $BASE_DIR/.share/java

      if [ -L "$BASE_DIR/.share/gradle" ]; then
        unlink "$BASE_DIR/.share/gradle" 
      fi
      ln -sf ${gradle}/lib/gradle $BASE_DIR/.share/gradle
      export GRADLE_HOME="$BASE_DIR/.share/gradle"

      export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:${
        pkgs.lib.makeLibraryPath [
          kotlin
          pkgs.libGL
          pkgs.xorg.libX11
          pkgs.fontconfig
        ]
      };
    '';
  }
