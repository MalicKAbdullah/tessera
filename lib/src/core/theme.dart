import 'package:core_theme/core_theme.dart';
import 'package:flutter/material.dart';

/// The Offlyne suite theme in Tessera's lime accent.
ThemeData tesseraTheme(Brightness brightness) =>
    AppTheme.build(brightness, accent: AppAccents.tessera);
