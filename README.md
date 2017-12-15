<img src="doc/logo.png" alt="logo" width="150" height="150")

##

Are you one of the many people who want to use their waisted time in the train to sleep a little extra, but does not try it because you are scared to sleep through your station?
With Sleepy, your fears are no longer needed! This app wakes you up when you approach your station. Share your favorite stations with the world so your friends can them!

The app is protected with a login system, that requires the user to be logged in before alarms can be set. The app uses gps to track where the user currently is and if a station is chosen, gives the distance to that station.
Then, the user can choose a wakeup distance, and set the timer. When the train gets closer then this wakeup distance, the alarm will go off with vibrations and sound.

All station data such as coordinates and names is retrieved from the NS API.

##

Functionality notes:

- App prevents non logged in users to be able to return to logged in pages.
- App reads GPS realtime and gives user distance to set station.
- If no station was set, no distance will be displayed.
- User can use the compass button to change the use of the search bar between searching stations or users. The suggestions will be changed accordingly.
- IMPORTANT: the NS API only allows 50000 requests per day. Although this is high enough for the current needs, the app does not request the API every time the app is launched. The app checks when the Firebase database was updated by a user the last time, and if that is longer then 4 days a go, only thén the Firebase containing all information as well will be updated.
- The user is not able to click on things before everything is set up correctly. The layout will only become visible once everything is loaded. In the meanwhile a spinner is showed.
- When the alarm is set, only the distance and a reset button are showing, next to the stunning logo of course.
- If the add button is clicked, the currently chosen station is added to the favorites. If the station was already in the list, it will not be duplicated.
- If a user is searched an clicked, a pop up window will show the list of favorites. But if the user has no favorites, only a toast will be given to inform the user about this.

##

To clarify the use of the app even more, some screenshots are provided:

The splash page:

![login screen](https://github.com/Segouta/Sleepy/blob/master/doc/splash.jpeg)
![login](https://github.com/Segouta/Sleepy/blob/master/doc/login.jpeg)
![signup](https://github.com/Segouta/Sleepy/blob/master/doc/signup.jpeg)

Loading screen:

![loading screen](https://github.com/Segouta/Sleepy/blob/master/doc/loading.jpeg)

Main activity:

![main activity](https://github.com/Segouta/Sleepy/blob/master/doc/mainstation.jpeg)

Searching for station:

![searching for station suggestions](https://github.com/Segouta/Sleepy/blob/master/doc/stationsuggestions.jpeg)

Adding and getting favorites:

![adding favorites](https://github.com/Segouta/Sleepy/blob/master/doc/confirmadd.jpeg)
![getting users favorites](https://github.com/Segouta/Sleepy/blob/master/doc/favoriteslist.jpeg)

Searching for user:

![searching for user empty](https://github.com/Segouta/Sleepy/blob/master/doc/mainuser.jpeg)
![searching for user suggestions](https://github.com/Segouta/Sleepy/blob/master/doc/usersuggestions.jpeg)

Displaying distance info:

![station chosen](https://github.com/Segouta/Sleepy/blob/master/doc/stationselected.jpeg)

Setting alarm:

![setting alarm popup](https://github.com/Segouta/Sleepy/blob/master/doc/summary.jpeg)

Sleeping screen:

![sleeping screen](https://github.com/Segouta/Sleepy/blob/master/doc/sleeping.jpeg)

Being woken up:

![alarm popup](https://github.com/Segouta/Sleepy/blob/master/doc/alarmdialog.jpeg)

