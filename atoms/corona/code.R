### VIASH START
par <- list(
  output = "plot.png",
)
### VIASH END

library(tidyverse)
library(EpiEstim)
library(openxlsx)
library(lubridate)

# collect incidence data
covid <- read_csv("https://epistat.sciensano.be/Data/COVID19BE_HOSP.csv")

incidence <-
  covid %>%
  group_by(DATE) %>% 
  summarise_if(is.numeric, sum) %>% 
  select(dates = DATE, I = NEW_IN)

# collect infector/infectee data
infections <- openxlsx::read.xlsx("https://assets.researchsquare.com/files/rs-18805/v3/dataset.xlsx") %>%
  mutate_at(vars(Infector.date.lwr, Infector.date.upr, Infectee.date), lubridate::mdy)

# interval-censored serial interval data:
# each line represents a transmission event, 
# EL/ER show the lower/upper bound of the symptoms onset date in the infector
# SL/SR show the same for the secondary case
# type has entries 0 corresponding to doubly interval-censored data
# (see Reich et al. Statist. Med. 2009).
si_data <- infections %>% transmute(
  EL = 0L,
  ER = 1L,
  SL = difftime(Infectee.date, Infector.date.upr, units = "days") %>% as.integer,
  SR = difftime(Infectee.date, Infector.date.lwr, units = "days") %>% as.integer,
  type = 0L
)

# Estimating R and the serial interval using data on pairs infector/infected
res <- estimate_R(
  incidence,
  method = "si_from_data",
  si_data = si_data,
  config = make_config()
)

png(filename=par$output) #, width=1000, height=700)
plot(res, "R", legend = FALSE)
# dev.off()
