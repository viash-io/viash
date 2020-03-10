
- Update Portash spec with some learnings
- Portash / Viash bundled with notebook
- Simple conversions, focus on Python:
  - Convert to script, static or with options
  - Convert to Python class/function to be called in notebook

@Robrecht: kan jij verder werken aan portash/viash approach? Is het realistisch om tegen volgende week op basis van de specs die we nu hebben in DESIGN.md een rudimentaire PoC op te bouwen?

* portash.yaml spec met python code (bijvoorbeeld dezelfde Titanic filter functionaliteit als wat we in R aan het doen waren)
* Nog geen automatische rendering van option args, dat doen we in volgende stap wel simpele viash.yaml 
* Eerste poc om portash.yaml + viash.yaml om te zetten in CLI script met de nodige option parsing
