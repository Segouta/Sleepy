Reviewed by David Van Grinsven:

Geen system.out of logd, behalve waar echt handig voor debug!
Geen ge-uncommende code!

In helpermethods:
- Let op je enter-gebruik. Af en toe is een enter echt niet nodig.
- stationListUpdate , XMLtoDB, stationsrequest, addToFavorites kunnen meer comments gebruiken
- Let op overbodige System.outs bijvoorbeeld in stationListRequest
- Er zitten ook nog ge-uncommende code bijvoorbeeld in lijn 108 en 123
- let op overbodige logs bijoorbeeld in line 110

In mainActivity:
- Enter tussen imports is niet nodig
- Let op je enter-gebruik. Af en toe is een enter echt niet nodig.
- Echt meer comments bij bijvoorbeeld het zetten van het alarm