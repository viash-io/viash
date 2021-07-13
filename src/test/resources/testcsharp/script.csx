using System.Reflection;

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

if (par.output != null)
{
    Log("Writing output to file");
    Type t = par.GetType();
    PropertyInfo [] pi = t.GetProperties();

    using (StreamWriter sw = File.AppendText(par.output))
    {
        foreach (PropertyInfo p in pi)
        {
            sw.WriteLine(p.Name + ": |" + p.GetValue(par) + "|");
        }

        sw.WriteLine("resources_dir: |" + resources_dir + "|");
    }
}
else
{
    Log("Printing output to console'");
    Type t = par.GetType();
    PropertyInfo [] pi = t.GetProperties();
    foreach (PropertyInfo p in pi)
    {
        System.Console.WriteLine(p.Name + ": |" + p.GetValue(par) + "|");
    }

    System.Console.WriteLine("resources_dir: |" + resources_dir + "|");
}