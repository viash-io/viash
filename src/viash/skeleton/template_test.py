import unittest
import os
from os import path
import subprocess


with open("input.txt", "w") as writer:
    writer.writelines(["one\n", "two\n", "three\n"])


class MyTest(unittest.TestCase):
    def test_component(self):
        out = subprocess.check_output(["./EXECUTABLE", "--input", "input.txt", "--output", "output.txt", "--option", "FOO-"]).decode("utf-8")

        self.assertTrue(path.exists("output.txt"))
        
        with open("output.txt", "r") as reader:
            lines = reader.readlines()
        
        self.assertEqual(lines, ["FOO-one\n", "FOO-two\n", "FOO-three\n"])
    
        
unittest.main()
