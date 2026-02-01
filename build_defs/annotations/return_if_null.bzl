"""
Build rule for transforming Java source files with @ReturnIfNull annotations.
This rule runs before javac to rewrite source code at compile time.
"""

def _return_if_null_transform_impl(ctx):
    """Transform Java source files by processing @ReturnIfNull annotations"""
    
    # Get the transformer tool
    transformer = ctx.executable._transformer
    
    # Process each input source file
    outputs = []
    for src_file in ctx.files.srcs:
        output = ctx.actions.declare_file(src_file.basename, sibling = src_file)
        
        ctx.actions.run(
            inputs = [src_file],
            outputs = [output],
            executable = transformer,
            arguments = [src_file.path, output.path],
            mnemonic = "ReturnIfNullTransform",
            progress_message = "Transforming @ReturnIfNull in %s" % src_file.short_path,
        )
        outputs.append(output)
    
    return [DefaultInfo(files = depset(outputs))]

return_if_null_transform = rule(
    implementation = _return_if_null_transform_impl,
    attrs = {
        "srcs": attr.label_list(
            allow_files = [".java"],
            mandatory = True,
            doc = "Java source files to transform",
        ),
        "_transformer": attr.label(
            executable = True,
            cfg = "exec",
            default = "//plugin:return_if_null_transformer",
            doc = "The ReturnIfNullSourceTransformer executable",
        ),
    },
    doc = "Transform Java sources by processing @ReturnIfNull annotations at compile time",
)
