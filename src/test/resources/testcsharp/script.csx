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
var resources_dir = ".";
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
            object[] array = (object[])p.GetValue(par);

            Output(p.Name + ": |" + string.Join(", ", array) + "|");
        }
        else
        {
            Output(p.Name + ": |" + p.GetValue(par) + "|");
        }
    }

    Output("resources_dir: |" + resources_dir + "|");

    t = meta.GetType();
    pi = t.GetProperties();

    foreach (PropertyInfo p in pi)
    {
        if (p.PropertyType.IsArray)
        {
            object[] array = (object[])p.GetValue(meta);

            Output("meta_" + p.Name + ": |" + string.Join(", ", array) + "|");
        }
        else
        {
            Output("meta_" + p.Name + ": |" + p.GetValue(meta) + "|");
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