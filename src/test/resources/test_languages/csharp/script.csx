using System.Reflection;
using System.IO;

// TODO: make sure certain stdout and stderrs are generated
// TODO: write stdout and stderr to file if --output and --log are specified

// VIASH START
var par = new {
  input = "input.txt",
  real_number = 123.987654,
  whole_number = 17,
  s = "...",
  truth = true,
  output = "output.txt",
  log = "log.txt",
  optional = "help",
  optional_with_default = "me"
};
var meta = new {
  resources_dir = "."
};
// VIASH END

void Log(string log)
{
    if (par.log != null)
    {
        using (StreamWriter sw = File.AppendText(par.log))
        {
            sw.WriteLine("INFO:" + log);
        }
    }
    else
    {
        Console.WriteLine("INFO:" + log);
    }
}

Log("Parsed input arguments.");

StreamWriter file;
if (par.output != null) {
    Log("Writing output to file");
    file = new(par.output);
}
else
{
    Log("Printing output to console'");
}

void Output(string str)
{
    if (par.output != null)
    {
        file.WriteLine(str);
    }
    else
    {
        System.Console.WriteLine(str);
    }
}

try
{
    Type t = par.GetType();
    PropertyInfo [] pi = t.GetProperties();

    foreach (PropertyInfo p in pi)
    {
        if (p.PropertyType.IsArray)
        {
            var array = p.GetValue(par) as Array;

            if (array.Length == 0)
                Output($"{p.Name}: |empty array|");
            else if (array is bool[])
            {
                var array2 = (array as bool[]).Select(x => x.ToString().ToLower());
                Output($"{p.Name}: |{string.Join(":", array2)}|");
            }
            else if (array is System.Int32[])
            {
                var array2 = array as System.Int32[];
                Output($"{p.Name}: |{string.Join(":", array2)}|");
            }
            else if (array is System.Int64[])
            {
                var array2 = array as System.Int64[];
                Output($"{p.Name}: |{string.Join(":", array2)}|");
            }
            else if (array is System.Double[])
            {
                var array2 = array as System.Double[];
                Output($"{p.Name}: |{string.Join(":", array2)}|");
            }
            else if (array is System.String[])
            {
                var array2 = array as System.String[];
                Output($"{p.Name}: |{string.Join(":", array2)}|");
            }
            else {
                Output($"{p.Name}: |{string.Join(":", array)}|");
            }
        }
        else
        {
            var value = p.GetValue(par);
            if (value is bool)
                Output($"{p.Name}: |{value.ToString().ToLower()}|");
            else
                Output($"{p.Name}: |{value}|");
        }
    }

    using(StreamReader input = new StreamReader(par.input))
    {
        Output($"head of input: |{input.ReadLine()}|");
    }
    using(StreamReader input = new StreamReader("resource1.txt"))
    {
        Output($"head of resource1: |{input.ReadLine()}|");
    }

    t = meta.GetType();
    pi = t.GetProperties();

    foreach (PropertyInfo p in pi)
    {
        if (p.PropertyType.IsArray)
        {
            object[] array = (object[])p.GetValue(meta);

            Output($"meta_{p.Name}: |{string.Join(", ", array)}|");
        }
        else
        {
            Output($"meta_{p.Name}: |{p.GetValue(meta)}|");
        }
    }
}
finally
{
    if (par.output != null)
    {
        file.Close();
    }
}