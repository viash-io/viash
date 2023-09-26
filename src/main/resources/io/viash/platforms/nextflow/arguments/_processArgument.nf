def _processArgument(arg) {
  arg.multiple = arg.multiple != null ? arg.multiple : false
  arg.required = arg.required != null ? arg.required : false
  arg.direction = arg.direction != null ? arg.direction : "input"
  arg.multiple_sep = arg.multiple_sep != null ? arg.multiple_sep : ":"
  arg.plainName = arg.name.replaceAll("^-*", "")

  if (arg.type == "file") {
    arg.must_exist = arg.must_exist != null ? arg.must_exist : true
    arg.create_parent = arg.create_parent != null ? arg.create_parent : true
  }

  // add default values to output files which haven't already got a default
  if (arg.type == "file" && arg.direction == "output" && arg.default == null) {
    def mult = arg.multiple ? "_*" : ""
    def extSearch = ""
    if (arg.default != null) {
      extSearch = arg.default
    } else if (arg.example != null) {
      extSearch = arg.example
    }
    if (extSearch instanceof List) {
      extSearch = extSearch[0]
    }
    def extSearchResult = extSearch.find("\\.[^\\.]+\$")
    def ext = extSearchResult != null ? extSearchResult : ""
    arg.default = "\$id.\$key.${arg.plainName}${mult}${ext}"
    if (arg.multiple) {
      arg.default = [arg.default]
    }
  }

  if (!arg.multiple) {
    if (arg.default != null && arg.default instanceof List) {
      arg.default = arg.default[0]
    }
    if (arg.example != null && arg.example instanceof List) {
      arg.example = arg.example[0]
    }
  }

  if (arg.type == "boolean_true") {
    arg.default = false
  }
  if (arg.type == "boolean_false") {
    arg.default = true
  }

  arg
}
