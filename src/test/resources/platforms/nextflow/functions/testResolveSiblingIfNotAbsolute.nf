
include { _resolveSiblingIfNotAbsolute } from params.workflowHelper

def testCases = [
    "/absolute/another/param_list.yaml": [
        "relative/path": ["/absolute/another/relative/path", "UnixPath"],
        "/absolute/path": ["/absolute/path", "UnixPath"],
        "s3://bucket/path": ["/bucket/path", "S3Path"],
        "../foo/bar": ["/absolute(/another/..)?/foo/bar", "UnixPath"],
        "file:///absolute/path": ["/absolute/path", "UnixPath"]
    ],
    "/absolute/../param_list.yaml": [
        "relative/path": ["/absolute(/../relative)?/path", "UnixPath"],
        "/absolute/path": ["/absolute/path", "UnixPath"],
        "s3://bucket/path": ["/bucket/path", "S3Path"],
        "../foo/bar": ["(/absolute/../..)?/foo/bar", "UnixPath"],
        "file:///absolute/path": ["/absolute/path", "UnixPath"]
    ],
    "relative/param_list.yaml": [
        "relative/path": ["relative/relative/path", "UnixPath"],
        "/absolute/path": ["/absolute/path", "UnixPath"],
        "s3://bucket/path": ["/bucket/path", "S3Path"],
        "../foo/bar": ["(relative/../)?foo/bar", "UnixPath"],
        "file:///absolute/path": ["/absolute/path", "UnixPath"]
    ],
    "s3://s3bucket/param_list.yaml": [
        "relative/path": ["/s3bucket/relative/path", "S3Path"],
        "/absolute/path": ["/absolute/path", "UnixPath"],
        "s3://bucket/path": ["/bucket/path", "S3Path"],
        "../foo/bar": ["/s3bucket/(../)?foo/bar", "S3Path"],
        "file:///absolute/path": ["/absolute/path", "UnixPath"]
    ],
    "https://remote_url/extra_dir/param_list.yaml": [
        // "relative/path": ["remote_url/extra_dir/relative/path", "XPath"],
        "/absolute/path": ["/absolute/path", "UnixPath"],
        "s3://bucket/path": ["/bucket/path", "S3Path"],
        // "../foo/bar": ["/remote_url/extra_dir/../foo/bar", "XPath"],
        "file:///absolute/path": ["/absolute/path", "UnixPath"]
    ],
    "/param_list.yaml": [
        "relative/path": ["/relative/path", "UnixPath"],
        "/absolute/path": ["/absolute/path", "UnixPath"],
        "s3://bucket/path": ["/bucket/path", "S3Path"],
        "../foo/bar": ["/(../)?foo/bar", "UnixPath"],
        "file:///absolute/path": ["/absolute/path", "UnixPath"]
    ],
    "/absolute/param_list.yaml": [
        "relative/path": ["/absolute/relative/path", "UnixPath"],
        "/absolute/path": ["/absolute/path", "UnixPath"],
        "s3://bucket/path": ["/bucket/path", "S3Path"],
        "../foo/bar": ["(/absolute/..)?/foo/bar", "UnixPath"],
        "file:///absolute/path": ["/absolute/path", "UnixPath"]
    ]

]


testCases.each { parentPath, providerTestCases ->
    providerTestCases.each { path, expected ->
        def expectedLocation = expected[0]
        def expectedClass = expected[1]
        def parentPathObject = file(parentPath, relative: true, hidden: true)
        def resolvedPath = _resolveSiblingIfNotAbsolute(path, parentPathObject)
        println("_resolveSiblingIfNotAbsolute(\"${parentPath}\", \"${path}\"): ${resolvedPath}, .getClass(): ${resolvedPath.getClass()}")
        assert resolvedPath.toString() ==~ expectedLocation
        assert resolvedPath.getClass().getSimpleName() == expectedClass
    }
    println("")
}