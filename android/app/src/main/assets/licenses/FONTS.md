# Bundled fonts

The font files in `res/font/` are static instances generated from the upstream
Google Fonts sources with fontTools `varLib.instancer`, then subset to Latin-1,
common punctuation/symbols and geometric shapes with `pyftsubset` (hinting removed).
All families are licensed under the SIL Open Font License, Version 1.1; each
family's license text is in this directory.

| Family | Upstream | License | Files |
|---|---|---|---|
| Doto (ROND=0) | https://github.com/google/fonts/tree/main/ofl/doto | SIL OFL 1.1 (`doto_OFL.txt`) | doto_300/400/500/700/900.ttf |
| Space Grotesk | https://github.com/google/fonts/tree/main/ofl/spacegrotesk | SIL OFL 1.1 (`space_grotesk_OFL.txt`) | space_grotesk_300/400/500/700.ttf |
| Inter Tight | https://github.com/google/fonts/tree/main/ofl/intertight | SIL OFL 1.1 (`inter_tight_OFL.txt`) | inter_tight_100/300/400/500/700/900.ttf |
| JetBrains Mono | https://github.com/google/fonts/tree/main/ofl/jetbrainsmono | SIL OFL 1.1 (`jetbrains_mono_OFL.txt`) | jetbrains_mono_300/400/500/700.ttf |
| Instrument Serif | https://github.com/google/fonts/tree/main/ofl/instrumentserif | SIL OFL 1.1 (`instrument_serif_OFL.txt`) | instrument_serif_400.ttf |
| Oswald | https://github.com/google/fonts/tree/main/ofl/oswald | SIL OFL 1.1 (`oswald_OFL.txt`) | oswald_300/400/500/700.ttf |
