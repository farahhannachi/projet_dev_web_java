# Field Reference Guide - Depot Form

## Quick Reference for Form Fields

When adding or editing a depot, here's what to put in each field:

### Required Fields (marked with *)

| Field | Format | Examples | Notes |
|-------|--------|----------|-------|
| **Nom** | Text (2-255 chars) | "Dépôt Centre", "Dépôt Nord" | Depot name |
| **Adresse** | Text (5-255 chars) | "Avenue Habib Bourguiba, 123" | Full address |
| **Ville** | Text (2-100 chars) | "Tunis", "Sfax", "Sousse" | City name |
| **Capacité** | Number (> 0) | 5000, 3000, 2500 | Storage capacity |
| **Responsable** | Text (2-255 chars) | "Ahmed Ben Ali", "Fatma Trabelsi" | Contact person |
| **Téléphone** | 10+ digits | 0123456789, +216 71 123 456 | French or international format |

### Optional Fields

| Field | Format | Examples | Notes |
|-------|--------|----------|-------|
| **Latitude** | Decimal (-90 to 90) | 36.8065, 36.8667 | Auto-filled from map |
| **Longitude** | Decimal (-180 to 180) | 10.1815, 10.1667 | Auto-filled from map |
| **Localisation** | Text (max 500 chars) | "Centre-Ville", "Zone Industrielle" | Location identifier |

## Field-Specific Guidelines

### 1. Téléphone (Phone Number)

**Accepted formats:**
- French: `0123456789`
- With spaces: `01 23 45 67 89`
- With dots: `01.23.45.67.89`
- International: `+216 71 123 456`
- With country code: `+21612345678`

**Not accepted:**
- Too short: `123`
- No digits: `abc`
- Missing leading 0 or +: `1234567890`

### 2. Latitude & Longitude

**Valid values:**
- Latitude: -90 to 90 (e.g., 36.8065 for Tunis)
- Longitude: -180 to 180 (e.g., 10.1815 for Tunis)

**How to fill:**
1. Leave empty (will be set to 0)
2. Enter manually (e.g., 36.8065)
3. Click "📍 Ouvrir la carte" and select location on map

**Examples:**
- Tunis: 36.8065, 10.1815
- Sfax: 34.7406, 10.7603
- Sousse: 35.8254, 10.6370

### 3. Localisation (Location Name)

**What to enter:**
- Neighborhood: "Centre-Ville", "Lac", "La Marsa"
- District: "Zone Industrielle", "Zone Commerciale"
- Landmark: "Près Hôpital", "Port de Commerce"
- Any descriptive identifier

**How to fill:**
1. Type manually (e.g., "Centre-Ville Tunis")
2. Click "📍 Ouvrir la carte", select location, click "Enregistrer"
3. Leave empty (will use city name as default)

**Max length:** 500 characters

## Common Examples

### Example 1: Complete Depot Entry
```
Nom:           Dépôt Tunis Centre
Adresse:       Avenue Habib Bourguiba, 123
Ville:         Tunis
Capacité:      5000
Responsable:   Ahmed Ben Ali
Téléphone:     +216 71 123 456
Latitude:      36.8065
Longitude:     10.1815
Localisation:  Centre-Ville Tunis
```

### Example 2: Minimal Entry (No Location)
```
Nom:           Dépôt Sfax Sud
Adresse:       Rue de la République, 456
Ville:         Sfax
Capacité:      3500
Responsable:   Fatma Trabelsi
Téléphone:     074234567
Latitude:      (leave empty)
Longitude:     (leave empty)
Localisation:  (leave empty - will use "Sfax")
```

### Example 3: Using Map for Location
1. Fill all required fields
2. Click "📍 Ouvrir la carte"
3. Zoom and click on the exact location
4. Click "Enregistrer" in the map window
5. Location name and coordinates auto-fill
6. Click "Enregistrer" in the main form

## Validation Rules

| Field | Min Length | Max Length | Pattern |
|-------|-----------|-----------|---------|
| Nom | 2 | 255 | Letters, numbers, spaces |
| Adresse | 5 | 255 | Any characters |
| Ville | 2 | 100 | Letters, spaces |
| Capacité | 1 | 7 digits | Numbers only, > 0 |
| Responsable | 2 | 255 | Letters, spaces |
| Téléphone | 10 digits | - | Starts with 0 or + |
| Latitude | - | - | -90 to 90 |
| Longitude | - | - | -180 to 180 |
| Localisation | 0 | 500 | Any characters |

## Tips

1. **Use the map**: Click "📍 Ouvrir la carte" for accurate coordinates
2. **Phone format**: Use +216 for Tunisian numbers
3. **Location names**: Be descriptive for easy identification
4. **Save often**: Click "Enregistrer" after filling each section
5. **Edit later**: You can always modify depot details

## Troubleshooting

**Error: "Format invalide. Utilisez 0123456789 ou +216 71 123 456"**
→ Check phone number format. Use 10 digits starting with 0, or +216 format.

**Error: "Format de latitude invalide"**
→ Latitude must be between -90 and 90. Use decimal format like 36.8065.

**Error: "Format de longitude invalide"**
→ Longitude must be between -180 and 180. Use decimal format like 10.1815.

**Error: "La capacité doit etre superieure a 0"**
→ Capacity must be a positive number greater than 0.

**Location not saving?**
→ Location is optional. If empty, the system uses the city name automatically.

## Quick Copy-Paste Examples

### Tunis Numbers
- 0123456789
- 071234567
- +216 71 123 456
- +21671234567

### Coordinates
- Tunis: 36.8065, 10.1815
- Sfax: 34.7406, 10.7603
- Sousse: 35.8254, 10.6370
- Ariana: 36.8667, 10.1667

### Location Names
- Centre-Ville
- Zone Industrielle
- Zone Commerciale
- Lac de Tunis
- La Marsa
- Carthage
- Près Hôpital