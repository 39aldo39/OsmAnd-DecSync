OsmAnd DecSync (proof of concept)
=================================

*This is a proof of concept, it does not work yet without root and heavy manual intervention*
---------------------------------------------------------------------------------------------

OsmAnd DecSync is an Android application which synchronizes the favorites of [OsmAnd](https://osmand.net) using [DecSync](https://github.com/39aldo39/DecSync) without requiring a server. To start synchronizing, all you have to do is synchronize your selected DecSync directory, using for example [Syncthing](https://syncthing.net).

Currently, root access and heavy manual intervention is required as OsmAnd does not yet support the reading of favorites. To test the application, you have to do the following:

- Manually grant OsmAnd DecSync storage access: App Info → Permissions → Storage → Allow.
- Manually copy `/data/data/net.osmand.plus/files/favourites_bak.gpx` to `/sdcard/DecSync/favourites.gpx`. This step has to be repeated every time before and after a sync.
