# Remotely
## Boosted Remote Reader / Emulator

This Android project is designed to try and reverse engineer the Boosted remote
so that we can get a board to think an app is a real remote.

This would allow us to have apps as backups if we break our remotes now that
Boosted are not operating as a business.

Also it's fun, and Ride could maybe gain some great new features as a result.

---

See below for documentation of what I've found out about how a Boosted remote advertises itself.

## Contents

- [Good to know](#good-to-know)
- [Why not iOS?](#why-not-ios)
- [Reverse engineering the remote](#reverse-engineering-the-remote)

## Good to know

- This project will not run on an emulator since an emulator doesn't have access to Bluetooth.
- You should only try and run the app in one mode at once. Quit the app and relaunch to use a different mode.
- Sometimes Bluetooth on Android can be finicky and a restart is sometimes the only way to fix it.
- Please make sure Bluetooth is already on before trying to use the app.
- Accept all permissions that come up otherwise it won't work.
- I've tested this on a Pixel 3a running Android 10 and a Nexus 5X running 8.1.

## Why not iOS?

Although the Core Bluetooth API on iOS is easier to use, because Apple do a lot of work for us like
queuing operations, this comes at a cost of being a less flexible API.

Specifically, this is a problem for this project because we need to be able to control as much
of the advertising data packets as possible, and Core Bluetooth doesn't give us any control over most
of the advertising data, like the "manufacturer specific data".

Even on Android, with just the public APIs it's not possible to get a 1:1 advertising packet (see below for more in-depth info on this),
but we can get it to match 60 bytes out of 62 bytes.

This probably means that if we can get it working for Android, iOS will never be able to have this
functionality, which is a shame.

## Reverse engineering the remote

I spent the best part of a day investigating just how a Boosted remote advertises itself which I'll document here so it doesn't get lost and to help others.

When the remote is in pairing mode (after pressing the power button 5 times), it advertises itself very slightly differently to how it does so in non-pairing mode.

Thankfully Android easily allows us to get the full array of bytes that a device advertises, which means
we can compare the bytes of what the remote advertises in pairing versus non-pairing modes.

### Pairing mode bytes

These are the bytes that are advertised in pairing mode:

> **NOTE**: The bytes come as one array, I've just sectioned them up with brackets here for readability.

```
Flags         DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL (reversed)          DATA_TYPE_MANUFACTURER_SPECIFIC_DATA    DATA_TYPE_MANUFACTURER_SPECIFIC_DATA    DATA_TYPE_LOCAL_NAME_COMPLETE

[(02) 01 06]  [(11) 06 66 7C 50 17 55 5E 22 8D E6 11 56 00 2C 77 C4 F4]   [(09) FF 02 00 03 03 02 FF FF FF]       [(08) FF 00 C6 11 AF 99 FF FF].         [(13) 09 42 6F 6F 73 74 65 64 52 6D 74 39 39 41 46 31 31 43 36]     [00 00]
```

> **NOTE**: You can see an explanation of the different sections further down.

### Non-pairing mode bytes

These are the bytes that are advertised in non-pairing mode:

```
Flags         DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL (reversed)          DATA_TYPE_MANUFACTURER_SPECIFIC_DATA    DATA_TYPE_MANUFACTURER_SPECIFIC_DATA    DATA_TYPE_LOCAL_NAME_COMPLETE

[(02) 01 06]  [(11) 06 66 7C 50 17 55 5E 22 8D E6 11 56 00 2C 77 C4 F4]   [(09) FF 00 00 03 03 02 FF FF FF]       [(08) FF 00 C6 11 AF 99 FF FF].         [(13) 09 42 6F 6F 73 74 65 64 52 6D 74 39 39 41 46 31 31 43 36]     [00 00]

```

If you compare the bytes above you'll see they're almost identical, except that one of the bytes in the first `DATA_TYPE_MANUFACTURER_SPECIFIC_DATA` section is `0x02` when advertising and `0x0` when not advertising.

Everything else remains exactly the same, so I'm assuming this is how a board knows whether to try to pair with a remote.

See further down to compare with the bytes that the app advertises for pairing and non-pairing modes, which are _almost_ identical to the remote's data.

### Sectioning the data up

The Bluetooth specification means that all devices have to advertise themselves in a common way. The way they do this
is by sectioning up the bytes in the advertising packet, which can be determined easily, and I'll show you how.

The way it works is the first byte in each section defines the _length of that section_. The second byte in each section
defines the _type of section_.

> **NOTE**: The bytes above are in hex values, **not decimal**, so 0x11 in hex is not 11 in decimal, it's 17 in decimal.

Take a look at the first byte in the "flags" section. It's `0x02`, which means, after that byte, there are a further 2 bytes (because 0x02 in hex means 2 in decimal). This is the _length indicator_ byte.

The second byte in the "flags" section is `0x01`, which indicates this section is for defining the flags in the advertising packet. This is the _type indicator_ byte, and the possible values of this byte are [defined here](https://www.bluetooth.com/specifications/assigned-numbers/generic-access-profile/).

Any bytes after that type indicator are the value of whatever the type of section is.

So the entire flags section is 3 bytes long, including the length indicator, it's a "flags" section, and the value is `0x06` (more on what that value means further down).

The next section length indicator is `0x11` which means it's 17 bytes (18 bytes including the length indicator). The type is `0x06` which means it's a 128 bit (16 bytes!) service UUID section, and the remaining 16 bytes are the actual UUID of the service being advertised.

### Section meanings and values
#### Flags

The first section the remote advertises is flags. We know this because the type indicator is 0x01.

Flags express to scanners the type of device that is advertising, and are [defined here](https://devzone.nordicsemi.com/f/nordic-q-a/29083/ble-advertising-data-flags-field-and-discovery). (If that link is dead, the code in this project also defines them)

Notice how the value of this section is only 1 byte, `0x06`, but that value doesn't exist in the link above. This is because
this is a special byte that can hold multiple combinations of the flags. You can check if this byte contains any of the above linked values by using a _bitwise-and_ check, which this project code has an example of doing.

In this case, `0x06` is a combination of two flags; `BLE_GAP_ADV_FLAG_LE_GENERAL_DISC_MODE` and `BLE_GAP_ADV_FLAG_BR_EDR_NOT_SUPPORTED`, basically meaning the remote is saying it's a general Bluetooth LE device and doesn't support BR/EDR (Bluetooth Basic Rate/ Enhanced Data Rate) mode.

#### Service UUIDs
The second section the remote advertises is `DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL`. We know this because the type indicator is `0x06`.

This section contains the bytes of the service UUID that is advertised to other scanning devices. Often you'll filter Bluetooth devices when scanning by UUIDs that they're advertising so that you can only get results for the devices you're interested in, rather than every nearby device.

In this case, the value of this section is `66 7C 50 17 55 5E 22 8D E6 11 56 00 2C 77 C4 F4`, which is the same bytes as the "general" Bluetooth service the remote has if you connect to it, except in reverse order: `F4C4772C-0056-11E6-8D22-5E5517507C66`.

I'm not sure why the bytes are reversed but it's part of the Bluetooth specification, and the system handles figuring this out for us.

> **NOTE**: `DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL` [apparently means](https://support.dialog-semiconductor.com/forums/post/dialog-smartbond-bluetooth-low-energy-–-software/incomplete-vs-complete-list-128-bit) that the remote is indicating that is is advertising this service, but actually has more services that can be discovered.

#### Manufacturer specific datas

The next two sections are both of type `DATA_TYPE_MANUFACTURER_SPECIFIC_DATA`, we know this because the type indicator is `0xFF` for those sections.

The "manufacturer specific data" sections are an opportunity for vendors to add their own proprietary data to the advertising packets that they know how to use.

The first 2 bytes of the value is meant to indicate the manufacturer ID, of which there is an [official list](https://www.bluetooth.com/specifications/assigned-numbers/company-identifiers/), but it looks like Boosted were using this to be able to indicate whether a remote is in pairing mode or not. In pairing mode, the manufacturer ID changes from `0x0 0x0` to `0x02 0x0`, but the rest of the data stays the same, and in the other section the id and data doesn't change.

The remaining bytes in this section are the actual bytes of the "manufacturer specific data", so in the first one that's `03 03 02 FF FF FF` and in the second one it's `C6 11 AF 99 FF FF`.

It appears that the data in the first one represents the firmware version of the remote, in reverse order. The firmware version of that remote is v2.3.3, and if you reverse the first three bytes you get `0x02 0x03 0x03`. I also tried this out with my other remote which has firmware v1.4.3 and the hex values (reversed) are `0x01 0x04 0x03`.

The data in the second one appears to be the same bytes (reversed) as in the remote's name, minus the "BoostedRmt" part. This is also the same value as the "serial number" characteristic value, although only one of my remotes has that characteristic (the one on firmware v2.3.3)

I'm not sure why there are also three `0xFF` bytes in the first one and another two `0xFF` bytes in the second one, but it's the same on both remotes.

#### Local name

The next section is of type `DATA_TYPE_LOCAL_NAME_COMPLETE`, we know this because the type indicator is `0x09`.

This section is to be able to advertise the name of the device doing the advertising. Since it's not possible to find out the name of a device without first connecting to it, this provides a way for scanning device to know the name (and show it to users so they can select the right one).

Not all advertising packets contain this, it's optional to add it on iOS and Android as well.

In this case, the value of this section is `42 6F 6F 73 74 65 64 52 6D 74 39 39 41 46 31 31 43 36` which is just the bytes of the text "BoostedRmt99AF11C6".

What I noticed is that if you remove "BoostedRmt" you're left with "99AF11C6", which interestingly are the same bytes found in the second manufacturer specific data section (in reverse). Although I'm not sure why there is also two extra `0xFF` bytes in the manufacturer data.

#### Padding bytes

The final section is just the bytes `0x0 0x0`, which means no length and no type. This basically means these are just padding bytes, to ensure that the payload is exactly the right size.

#### Maximum bytes

The Bluetooth specification requires that advertisement data be no larger than 31 bytes. But if you count the bytes above, it's actually 62 bytes.

This is because you can actually split your data up into two parts; the initial advertisement data, and a scan response (data that gets sent to a device that discovers it).

Both of these can only be a maximum of 31 bytes, which combined means you can essentially advertise up to 62 bytes of information, which is what Boosted are doing with the remote.

The extra padding bytes at the end I assume are because the packet has to exactly match 31 bytes, so because the scan response only consist of 29 bytes, two extra empty bytes are added to make it add up to 31.

Because we know that the maximum value can only be 31 bytes for each, we know then which bits of information the remote is advertising and adding to the scan response.

The first 3 sections (flags, uuid and manufacturer data 1) are in the advertisement data, and the remaining sections (manufacturer data 2 and local name, plus the padding bytes) are in the scan response.

### Pretending to advertise as the remote as an app

So we know now what the remote itself advertises, so we can try and advertise as an app in the same way to trick a board into thinking the app is a real remote.

This is as close as I could get the bytes in pairing and non pairing modes:

```
[02 01 02] [11 07 66 7C 50 17 55 5E 22 8D E6 11 56 00 2C 77 C4 F4]  [09 FF 00 00 03 03 02 FF FF FF]       [08 FF 00 C6 11 AF 99 FF FF]            [13 09 42 6F 6F 73 74 65 64 52 6D 74 39 39 41 46 31 31 43 36]     [00 00]
```

```
[02 01 02] [11 07 66 7C 50 17 55 5E 22 8D E6 11 56 00 2C 77 C4 F4]  [09 FF 02 00 03 03 02 FF FF FF]       [08 FF 00 C6 11 AF 99 FF FF]            [13 09 42 6F 6F 73 74 65 64 52 6D 74 39 39 41 46 31 31 43 36]     [00 00]
```

If you compare with the bytes from the real remote, you'll see there are two small differences.

The app advertises with the flag value of `0x02` rather than the remote's which is `0x06`, which means the app is only indicating that it is a general Bluetooth LE device and nothing else.

Technically this shouldn't be a reason for the board to not think it's a capable remote, but perhaps the data has to match exactly for the board to recognise it as a Boosted remote. Unfortunately, with the public Bluetooth APIs available on Android, I can't find a way to set this flag value to match explicitly.

The other difference is the type indicator for the UUIDs section is `0x07` and not `0x06`. The actual value bytes are exactly the same, but the remote advertises it as partial (`DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL`) and the app advertises it as complete (`DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE`). Again, unfortunately I can't see a way to override this byte using the public Android Bluetooth APIs. You can read more on the difference between these two [here](https://support.dialog-semiconductor.com/forums/post/dialog-smartbond-bluetooth-low-energy-–-software/incomplete-vs-complete-list-128-bit).

This means that 60 out of the 62 bytes match, and this could potentially be a reason why I can't get the board to try to connect to the app as a remote when they're both in pairing mode.

I really hope it's actually something else that's the problem, that we can fix, but I'm not sure what it is.

In any case, we've learnt a lot about how a remote advertises itself, and also how Bluetooth devices are required to advertise themselves generally. Plus if you're new to engineering or haven't worked with raw bytes before, you'll hopefully have learnt a bit about how to understand them, so it's not totally wasted effort in any case.
