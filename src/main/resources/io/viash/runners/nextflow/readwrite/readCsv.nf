
def readCsv(file_path) {
  def output = []
  def inputFile = file_path !instanceof Path ? file(file_path, hidden: true) : file_path

  // todo: allow escaped quotes in string
  // todo: allow single quotes?
  def splitRegex = java.util.regex.Pattern.compile(''',(?=(?:[^"]*"[^"]*")*[^"]*$)''')
  def removeQuote = java.util.regex.Pattern.compile('''"(.*)"''')

  java.nio.file.Files.newBufferedReader(inputFile).withCloseable { br ->
    def row = 0
    def header = null
    def line

    while (header == null && (line = br.readLine()) != null) {
      if (!line.startsWith("#")) {
        header = splitRegex.split(line, -1).collect { field ->
          def m = removeQuote.matcher(field)
          m.find() ? m.replaceFirst('$1') : field
        }
      }
      row++
    }
    assert header != null : "CSV file should contain a header"

    while ((line = br.readLine()) != null) {
      row++
      if (!line.startsWith("#")) {
        def predata = splitRegex.split(line, -1)
        def data = predata.collect { field ->
          if (field == "") {
            return null
          }
          def m = removeQuote.matcher(field)
          if (m.find()) {
            return m.replaceFirst('$1')
          } else {
            return field
          }
        }
        assert header.size() == data.size() : "Row $row should contain the same number as fields as the header"

        def dataMap = [header, data].transpose().collectEntries().findAll { it.value != null }
        output.add(dataMap)
      }
    }
  }

  output
}
