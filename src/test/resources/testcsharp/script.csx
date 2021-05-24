using System.Reflection;

// TODO: make sure certain stdout and stderrs are generated
// TODO: write stdout and stderr to file if --output and --log are specified

// VIASH START
// VIASH END

Type t = par.GetType();
PropertyInfo [] pi = t.GetProperties();
foreach (PropertyInfo p in pi)
{
    System.Console.WriteLine(p.Name + ": |" + p.GetValue(par) + "|");
}

System.Console.WriteLine("resources_dir: |" + resources_dir + "|");

