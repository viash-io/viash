import unittest
import os
from os import path
import subprocess

# check whether platform is docker
if os.getenv("VIASH_PLATFORM") == "docker":
  docker_args = ["--data", os.getcwd()]
  data_dir = "/data"
else:
  docker_args = []
  data_dir = os.getcwd()

class TestCheckOutputs(unittest.TestCase):
  
  def test_building_components(self):
    out = subprocess.check_output(["testpython", "---setup"], stderr=subprocess.STDOUT).decode("utf-8")
    self.assertNotRegex(out, 'error')
    
  def test_check_output(self):
    out = subprocess.check_output([
      "testpython", "help", "--real_number", "10.5", "--whole_number=10", 
      "-s", "you shall#not$pass", "--truth",
      "--output", data_dir + "/output.txt", "--log", data_dir + "/log.txt",
      "--optional", "foo", "--optional_with_default", "bar"
      ] + docker_args
    ).decode("utf-8")
    
    self.assertTrue(path.exists("output.txt"))
    fo = open("output.txt")
    output = "\n".join(fo.readlines())
    
    self.assertRegex(output, 'input: "help"')
    self.assertRegex(output, 'real_number: "10.5"')
    self.assertRegex(output, 'whole_number: "10"')
    self.assertRegex(output, 's: "you shall#not\\$pass"')
    self.assertRegex(output, 'truth: "True"')
    self.assertRegex(output, 'output: ".*/output.txt"')
    self.assertRegex(output, 'log: ".*/log.txt"')
    self.assertRegex(output, 'optional: "foo"')
    self.assertRegex(output, 'optional_with_default: "bar"')
    fo.close()
    
    self.assertTrue(path.exists("log.txt"))
    fo = open("log.txt")
    log = "\n".join(fo.readlines())
    
    self.assertRegex(log, 'Parsed input arguments.')
    fo.close()
    
  def test_check_output_with_minimal_args(self):
    output = subprocess.check_output(
      ["testpython", "test", "--real_number", "123.456",
      "--whole_number", "789", "-s", "my weird string"] + docker_args
    ).decode("utf-8")
    
    self.assertRegex(output, 'input: "test"')
    self.assertRegex(output, 'real_number: "123.456"')
    self.assertRegex(output, 'whole_number: "789"')
    self.assertRegex(output, 's: "my weird string"')
    self.assertRegex(output, 'truth: "False"')
    self.assertRegex(output, 'output: "None"')
    self.assertRegex(output, 'log: "None"')
    self.assertRegex(output, 'optional: "None"')
    self.assertRegex(output, 'optional_with_default: "The default value."')
    self.assertRegex(output, 'Parsed input arguments.')

unittest.main()
