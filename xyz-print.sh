#!/bin/bash
sed -ne 's/filament used = \(.*\)mm.*/total_filament = \1/p' "$*" > "$*".xyz
sed  '/^$/d' $* >> "$*".xyz
java -jar target/xyz-print-1.0-SNAPSHOT.jar $*.xyz