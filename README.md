# clj-pdfbooklet

WIP. Small app for preparing ready to print pdf in booklet format.

## Installation

Download from https://github.com/gigal00p/clj-pdfbooklet

## Usage

Simply invoke from CLI:

    $ java -jar clj-pdfbooklet-0.1.0-standalone.jar [args]

## Options

* `-i` - input pdf document
* `-o` - output directory where booklet will be written
* `-s` - number of pages of target booklet chunk

## Examples

`java -jar clj-pdfbooklet-0.1.0-SNAPSHOT-standalone.jar -i C:\book.pdf -o C:\Users\name\Downloads -s 315`

### Bugs

Please report bugs via Github.

## License

Copyright © 2020 Krzysztof Walkiewicz

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
