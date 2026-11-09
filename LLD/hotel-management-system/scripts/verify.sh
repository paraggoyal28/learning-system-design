#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
BUILD="$ROOT/build/classes"
rm -rf "$ROOT/build"
mkdir -p "$BUILD"
find "$ROOT/src/main/java" "$ROOT/src/test/java" -name '*.java' -print > "$ROOT/build/sources.list"
javac --release 17 -Xlint:all -d "$BUILD" @"$ROOT/build/sources.list"
java -cp "$BUILD" com.example.hotelmanagement.HotelManagementBehaviorTest
java -cp "$BUILD" com.example.hotelmanagement.HotelManagementApplication
