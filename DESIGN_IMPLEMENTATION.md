# Event Lottery Design Implementation

## Summary
Successfully implemented the Event Lottery screen design with all necessary drawable resources and layout files.

## Files Created

### Layout Files
1. **activity_event_lottery.xml** - Main layout file for the Event Lottery screen
   - Location: `/app/src/main/res/layout/activity_event_lottery.xml`

### Drawable Resources (Backgrounds)
1. **bg_search.xml** - Rounded background for search bar (#1B1B1B, 24dp radius)
2. **bg_chip.xml** - Rounded background for filter chips (#1B1B1B, 18dp radius)
3. **bg_card.xml** - Rounded background for event cards (#151515, 20dp radius)
4. **bg_spots.xml** - Blue background for "spots left" pill (#0072A4, 20dp radius)
5. **bg_scan_qr.xml** - Cyan background for Scan QR button (#00D9C5, 30dp radius)
6. **bg_circle_dark.xml** - Circular dark background for profile icon (#1B1B1B)

### Icon Drawables
1. **ic_person.xml** - Person/profile icon
2. **ic_search.xml** - Search magnifying glass icon
3. **ic_arrow_drop_down.xml** - Dropdown arrow for filter chips
4. **ic_home.xml** - Home icon for bottom navigation
5. **ic_calendar.xml** - Calendar icon for "My Events" in bottom navigation
6. **ic_qr_code.xml** - QR code scanner icon for the floating button

### Sample Event Images
1. **sample_event_1.xml** - Teal placeholder (#127A70)
2. **sample_event_2.xml** - Beige placeholder (#D9C7AF)
3. **sample_event_3.xml** - Light beige placeholder (#E6D7CF)

## Design Features Implemented

### Top Bar
- "Event Lottery" title (bold, 22sp, white)
- Profile icon in circular dark background (32dp)

### Search Bar
- Rounded dark background with search icon
- "Search events" hint text (#BFBFBF)

### Filter Chips
- Three horizontally scrollable chips: Interest, Date, Location
- Each with dropdown arrow icon
- Dark background with white text

### Event Cards (3 sample cards)
Each card includes:
- Colored header image (150dp height)
- Event title (bold, 16sp)
- Event date (13sp, gray)
- Blue "spots left" pill
- "Req: US 01.01.01" text on the right

Sample events:
1. Tech Conference 2024 - Oct 26, 2024 - 1000 spots left
2. Music Festival - Nov 15, 2024 - 500 spots left
3. Art Exhibition - Dec 5, 2024 - 200 spots left

### Bottom Navigation
- Home (active - white)
- My Events (inactive - gray)
- Profile (inactive - gray)

### Floating Scan QR Button
- Cyan background (#00D9C5)
- Black text
- QR code icon
- Positioned above bottom bar

## Color Scheme
- Background: #0F0F0F (near black)
- Card/Surface: #151515 (dark gray)
- Search/Chip: #1B1B1B (lighter dark gray)
- Accent Blue: #0072A4 (spots pill)
- Accent Cyan: #00D9C5 (scan button)
- Text Primary: White
- Text Secondary: #BFBFBF
- Text Tertiary: #A4A4A4
- Inactive Nav: #AFAFAF

## Usage
To use this layout in your app:
1. Reference it in an Activity or Fragment
2. Replace sample event images with actual event photos
3. Connect the search bar and filter chips to your data filtering logic
4. Implement click handlers for navigation items and the Scan QR button
5. Populate the event cards with real data from your backend/database

All files are ready to use and have no errors!

