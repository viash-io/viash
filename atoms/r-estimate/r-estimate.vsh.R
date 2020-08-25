#' functionality:
#'   name: r-estimate
#'   version: 1.1
#'   description: |
#'   
#'     Estimate the R value based on the vignette: 
#'       https://cran.r-project.org/web/packages/EpiEstim/vignettes/demo.html
#'     As input, the following are used:
#'       https://epistat.sciensano.be/Data/COVID19BE_HOSP.csv
#'       https://assets.researchsquare.com/files/rs-18805/v3/dataset.xlsx
#'   
#'   arguments:
#'   - name: "--output"
#'     alternatives: ["-o"]
#'     type: file
#'     description: The path to the output plot file.
#'     default: output.png
#'     required: true
#'     direction: output
#' platforms:
#' - type: docker
#'   image: rocker/tidyverse
#'   r:
#'     cran:
#'     - EpiEstim
#'     - openxlsx
#'     - lubridate
#'     - patchwork
#'   workdir: /app
#' - type: native
#'   r:
#'     cran:
#'     - EpiEstim
#'     - openxlsx
#'     - lubridate
#'     - patchwork

library(tidyverse)
library(EpiEstim)
library(openxlsx)
library(lubridate)
library(patchwork)

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

# make nicer plots than the ones proposed by EpiEstim
plots <- map(c("incid", "R", "SI"), function(what) {
  g <- plot(res, what = what) + theme_bw()
  if (what %in% c("incid", "R")) {
    g <- g + 
      scale_x_date(breaks = "1 week") +
      theme(axis.text.x = element_text(angle = 35, hjust = 1))
  }
  g
})

summary(res$R)
print(res$R)

g <- wrap_plots(plots, ncol = 1)
ggsave(par$output, g, height = 8, width = 8)
