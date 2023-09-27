
def readCsv(file_path) {
  def output = []
  def inputFile = file_path !instanceof Path ? file(file_path) : file_path

  // todo: allow escaped quotes in string
  // todo: allow single quotes?
  def splitRegex = java.util.regex.Pattern.compile(''',(?=(?:[^"]*"[^"]*")*[^"]*$)''')
  def removeQuote = java.util.regex.Pattern.compile('''"(.*)"''')

  def br = java.nio.file.Files.newBufferedReader(inputFile)

  def row = -1
  def header = null
  while (br.ready() && header == null) {
    def line = br.readLine()
    row++
    if (!line.startsWith("#")) {
      header = splitRegex.split(line, -1).collect{field ->
        m = removeQuote.matcher(field)
        m.find() ? m.replaceFirst('$1') : field
      }
    }
  }
  assert header != null: "CSV file should contain a header"

  while (br.ready()) {
    def line = br.readLine()
    row++
    if (line == null) {
      br.close()
      break
    }

    if (!line.startsWith("#")) {
      def predata = splitRegex.split(line, -1)
      def data = predata.collect{field ->
        if (field == "") {
          return null
        }
        m = removeQuote.matcher(field)
        if (m.find()) {
          return m.replaceFirst('$1')
        } else {
          return field
        }
      }
      assert header.size() == data.size(): "Row $row should contain the same number as fields as the header"
      
      def dataMap = [header, data].transpose().collectEntries().findAll{it.value != null}
      output.add(dataMap)
    }
  }

  output
}
