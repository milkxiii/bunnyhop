bunnyhop!
6/18/2023
ics3u final isu game

******************************************************************** Hints on how to play ********************************************************************
	
	- Click play button to play 4 levels in order

	- Click on the star icon in bottom right corner of home page to select a level (you don't have to unlock or play levels in order)

	- ESC button in rules/credits/level selection/enter name brings you page to homepage, ESC in middle of game toggles on/off the pause menu

	- Restart level by clicking on pause button/ESC and clicking restart button
	
	- Make sure you're releasing the previous arrow key before trying to click another key to move the bunny (not allowed to hold keys and make bunny move - bunny moves only 1 square each time)

******************************************************************** Known bugs / errors ********************************************************************

	- Sometimes if you spam the keys too much, the bunny will walk misaligned to the track (everything becomes one square off and the bunny is basically shifted a square away from the actual path). It has only happened a few times to me when I was testing though, and I'm not sure why it happens or which piece of code made that happen. But it's probably because there's too many keys being clicked at once so some of the logic gets messed up when it's checking if it's a valid move and the arrays aren't updated correctly.

	- The timer isn't very accurate and each second in the timer is a bit slower than an actual second. This is because I used thread.sleep(9) and counted each refresh as 1/100 of a second, which was the closest I could get since thread.sleep(8) was too fast and thread.sleep(10) was way too slow

	- If the time is above 60 minutes, the display formatting would look weird (although hopefully it doesn't take 60 minutes to complete one level), and if your rank is 3 digits it wouldn't show up in the winning screen leaderboard (but hopefully there won't be over 100 scores)

	- In very rare cases, the random path generated from the setRandomPaths method might be able to have an alternate correct path (different from the originally planned one), but it still won't allow the player to pass. The player would still have to find the originally intended path to pass the level. Maybe instead of generating a random path every time, I could set my own path/orientation manually for the player to use. Also, that would mean each time you play it you would get the exact same layout of the board (which could be a good thing if you want to compare the rankings/times more fairly, but also it might be more fun if it's random every time!)	
