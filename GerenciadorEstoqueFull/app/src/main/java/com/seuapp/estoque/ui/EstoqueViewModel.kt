package com.seuapp.estoque.ui

/**
 * Provides a typealias so that classes referencing EstoqueViewModel via
 * the com.seuapp.estoque.ui package name will resolve to the actual
 * implementation defined in com.seuapp.estoque.  This avoids type
 * mismatches when wiring Compose screens to the shared view model.
 */
typealias EstoqueViewModel = com.seuapp.estoque.EstoqueViewModel