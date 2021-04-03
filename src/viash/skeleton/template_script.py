print("This is a skeleton component")
print("The arguments are:")
print(" - input: ", par["input"])
print(" - output: ", par["output"])
print(" - option: ", par["option"])
print("")


with open(par["input"], "r") as reader, open(par["output"], "w") as writer:
    lines = reader.readlines()
    
    new_lines = [par["option"] + x for x in lines]
    
    writer.writelines(new_lines)
