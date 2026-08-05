<h1 align="center">AgentX Discord Bot</h1> 
<img width="960" height="540" align="center" alt="agency-banner" src="https://github.com/user-attachments/assets/8367f408-9829-4361-8d3b-817272b3c73e" />

AgentX is a custom Discord bot for the \<Agency\> WoW guild, used for guild management.

## Features and Commands:
### Post-Out System
Users are able to log absences for raid nights using the `/postout` commands, which will push notifications to the Post Out channel. Weekly reports will be sent to guild officers on who is absent for a raid week on the reset day for their region.
- `/postout create` - Create a new post out, provides buttons for the current and next raid week with select-able raid days to streamline creation. Alternatively users can select a "Future date" and provide a list of dates via textbox. Allows an optional note for absence reason after selecting days/dates.
- `/postout view` - Provides the user with a list of their created post outs.
- `/postout delete` - Allows the user to delete their post outs via drop-down menu.
