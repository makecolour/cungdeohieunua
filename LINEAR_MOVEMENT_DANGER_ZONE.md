# Linear Movement Danger Zone - Accurate Implementation

## Problem Analysis
NPCs move in straight lines for 9 cells and then go back. With a 3x3 attack range, the danger zone should be calculated as:
- **Movement path:** 9 cells long × 3 cells wide = 27 cells
- **Attack range extensions:** 1 cell extension at start and end = 6 additional cells
- **Total per NPC:** 33 cells (much more realistic than previous 81 or 25)

## Mathematical Model

### Linear Movement Pattern:
```
NPC at position (x,y) moves in a straight line:
- Horizontal: from (x-4, y) to (x+4, y) [9 cells total]
- OR Vertical: from (x, y-4) to (x, y+4) [9 cells total]
```

### Attack Range Coverage:
```
For 3x3 attack range at any position along the path:
- Center cell + 1 cell on each side = 3 cells wide
- Attack range extends 1 cell beyond movement endpoints
```

### Danger Zone Calculation:
```
Main corridor: 9 cells (length) × 3 cells (width) = 27 cells
Extensions: 2 endpoints × 3 cells (width) = 6 cells
Total: 33 cells per NPC
```

## Implementation Details

### 1. Movement Corridor
- Models 9-cell linear movement path (horizontal and vertical possibilities)
- Applies 3-cell width (1 cell on each side of movement line)
- Centered around NPC's current position

### 2. Attack Range Extensions
- Adds 1-cell extensions at both ends of movement path
- Accounts for attack range extending beyond movement endpoints
- Maintains 3-cell width for extensions

### 3. Directional Coverage
- Considers both horizontal and vertical movement possibilities
- Uses worst-case scenario for pathfinding safety
- Avoids assumption of specific movement direction

## Expected Results

### Before Fix:
```
- Too conservative: 81 cells per NPC (predicted all directions)
- Too optimistic: 25 cells per NPC (only current position)
```

### After Fix:
```
NPC GHOST at (10,10) creates 33 danger cells
- 27 cells for 9×3 movement corridor
- 6 cells for attack range extensions
Total threat cells: More accurate and reasonable
```

## Code Structure:
```java
// Main movement corridor (9×3)
for (pathStep = 0; pathStep < 9; pathStep++) {
    for (sideOffset = -1; sideOffset <= 1; sideOffset++) {
        // Add horizontal and vertical possibilities
    }
}

// Attack range extensions (1 cell at each end)
for (ext = 1; ext <= 1; ext++) {
    for (sideOffset = -1; sideOffset <= 1; sideOffset++) {
        // Add start and end extensions
    }
}
```

## Benefits:
1. **Accurate Threat Model** - Matches actual NPC behavior (9-cell linear movement)
2. **Proper Attack Range** - Accounts for 3x3 attack extending beyond movement path
3. **Balanced Caution** - Not too conservative (81) or too aggressive (25)
4. **Directional Coverage** - Handles both horizontal and vertical movement possibilities

The bot should now have a much more accurate understanding of NPC threats while still maintaining appropriate safety margins for effective gameplay!
