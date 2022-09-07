import unittest
import os
from os import path
import subprocess

class TestCheckOutputs(unittest.TestCase):
  def test_check_output(self):
    out = subprocess.check_output([
        "./testpython", 
        "help", "--real_number", "10.5", "--whole_number=10", "-s", "a string with spaces",
        "--truth", "--optional", "foo", "--optional_with_default", "bar",
        "a", "b", "c", "d",
        "--output", "./output.txt", "--log", "./log.txt",
        "--multiple", "one", "--multiple=two", 
        "e", "f"
      ]
    ).decode("utf-8")
    
    self.assertTrue(path.exists("output.txt"))
    fo = open("output.txt")
    output = "\n".join(fo.readlines())
    fo.close()
    
    self.assertRegex(output, 'input: \\|help\\|')
    self.assertRegex(output, 'real_number: \\|10.5\\|')
    self.assertRegex(output, 'whole_number: \\|10\\|')
    self.assertRegex(output, 's: \\|a string with spaces\\|')
    self.assertRegex(output, 'truth: \\|True\\|')
    self.assertRegex(output, 'output: \\|..*/output.txt\\|')
    self.assertRegex(output, 'log: \\|..*/log.txt\\|')
    self.assertRegex(output, 'optional: \\|foo\\|')
    self.assertRegex(output, 'optional_with_default: \\|bar\\|')
    self.assertRegex(output, 'multiple: \\|\\[\'one\', \'two\'\\]\\|')
    self.assertRegex(output, 'multiple_pos: \\|\\[\'a\', \'b\', \'c\', \'d\', \'e\', \'f\'\\]\\|')
    self.assertRegex(output, 'meta_resources_dir: \\|..*\\|')
    self.assertRegex(output, 'meta_functionality_name: \\|testpython\\|')
    self.assertRegex(output, 'meta_n_proc: \\|None\\|')
    self.assertRegex(output, 'meta_memory_b: \\|None\\|')
    self.assertRegex(output, 'meta_memory_kb: \\|None\\|')
    self.assertRegex(output, 'meta_memory_mb: \\|None\\|')
    self.assertRegex(output, 'meta_memory_gb: \\|None\\|')
    self.assertRegex(output, 'meta_memory_tb: \\|None\\|')
    self.assertRegex(output, 'meta_memory_pb: \\|None\\|')

    
    self.assertTrue(path.exists("log.txt"))
    fo = open("log.txt")
    log = "\n".join(fo.readlines())
    fo.close()
        
    self.assertRegex(log, 'Parsed input arguments.')
    
  def test_check_output_with_minimal_args(self):
    output = subprocess.check_output(
      ["./testpython", "test", "--real_number", "123.456",
      "--whole_number", "789", "-s", 'my$weird#string"""\'\'\'`\\@',
      '---n_proc', '666', '---memory', '100PB']
    ).decode("utf-8")
    
    self.assertRegex(output, 'input: \\|test\\|')
    self.assertRegex(output, 'real_number: \\|123.456\\|')
    self.assertRegex(output, 'whole_number: \\|789\\|')
    self.assertRegex(output, 's: \\|my\\$weird#string"""\'\'\'`\\\\@\\|')
    self.assertRegex(output, 'truth: \\|False\\|')
    self.assertRegex(output, 'output: \\|None\\|')
    self.assertRegex(output, 'log: \\|None\\|')
    self.assertRegex(output, 'optional: \\|None\\|')
    self.assertRegex(output, 'optional_with_default: \\|The default value.\\|')
    self.assertRegex(output, 'multiple: \\|None\\|')
    self.assertRegex(output, 'multiple_pos: \\|None\\|')
    self.assertRegex(output, 'meta_n_proc: \\|666\\|')
    self.assertRegex(output, 'meta_memory_b: \\|112589990684262400\\|')
    self.assertRegex(output, 'meta_memory_kb: \\|109951162777600\\|')
    self.assertRegex(output, 'meta_memory_mb: \\|107374182400\\|')
    self.assertRegex(output, 'meta_memory_gb: \\|104857600\\|')
    self.assertRegex(output, 'meta_memory_tb: \\|102400\\|')
    self.assertRegex(output, 'meta_memory_pb: \\|100\\|')
    self.assertRegex(output, 'Parsed input arguments.')
  
  def test_check_error(self):
    output = subprocess.run(
      ["./testpython", "test", "--real_number", "abc",
      "--whole_number", "789", "-s", "my weird string"], 
      capture_output=True
    )
    self.assertTrue(output.returncode > 0)
    
unittest.main()
