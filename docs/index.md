# Little Logistics Guide

Little Logistics is a mod for Minecraft Forge focused on efficient, affordable, long-distance transport. This guide does not cover recipes, to check item recipes use JEI or a similar mod.

<video src="https://user-images.githubusercontent.com/31002977/150042817-908a75b5-2802-4c83-a13d-0fbecc0ec94c.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/150042817-908a75b5-2802-4c83-a13d-0fbecc0ec94c.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>

## Video Guide

Check out the Bit-By-Bit Mischief of Mice!
<iframe width="560" height="315" src="https://www.youtube.com/embed/4hI99VUqczw" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

<br/>

## In Game Book

Install [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli) to get access to the in-game version of this guide. Can be crafted using a book, a chest, and a compass (shapeless).

<img src="https://user-images.githubusercontent.com/31002977/151086871-fc13047b-a52a-4bb0-84f9-c4f7ab76e2c0.png" style="max-height:35vh;">


## Vessels

There are two types of vessels in this mod: tugs and barges. Tugs can move the barges when chained, and barges and do all sort of stuff. Tugs can also be moved manually using leads.

### Tugs

#### Steam Tug

A basic tugboat that can operate on any furnace fuel. Fuel can be loaded with hoppers placed on top of tug docks.

<img src="https://user-images.githubusercontent.com/31002977/150037890-3bd23e8c-8aea-4910-85c3-34ba49c7504c.png" style="max-height:35vh;">

#### Energy Tug

A tugboat powered by Forge Energy, can be charged when docked with a Vehicle Charger. The tug has an extra slot for a capacitor, this is useful if the tug ran out of energy mid-way and needs to be recharged.

<img src="https://user-images.githubusercontent.com/31002977/161363667-f74d6fb7-8fdb-40b5-9be7-f59017e08c38.png" style="max-height:35vh;">

#### Tug route

This item stores a route for the tug. When the item is placed in the tug's route slot, the tug will follow the route in order from node 0. Right click at any coordinate to add a node to the **back** of the tug route (cannot add to the middle). Right click at the location of any existing node to remove it. Shift right click to bring up the menu that lets you modify the order of nodes or rename them. The route can be copied by placing it together with an empty one in a crafting grid. 

<video src="https://user-images.githubusercontent.com/31002977/149873867-d83fec24-2a14-4774-b3f3-feaa08d27440.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/149873867-d83fec24-2a14-4774-b3f3-feaa08d27440.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>

### Barges

#### Chest Barge

A simple barge that holds items, very similar to vanilla minecart. Must be docked and part of a tug-train to interface with hoppers.

<img src="https://user-images.githubusercontent.com/31002977/150036819-99629a2b-d3db-4058-aa16-4bd7bed7b1ac.png" style="max-height:35vh;">

#### Seater Barge

A simple barge that seats one player.

#### Auto-Fishing Barge

Performs fishing passively, uses vanilla fishing rod loot table. Needs to be in open water for nets to be deployed. Can be unloaded with hoppers and barge docks. Must be docked and part of a tug-train to interface with hoppers.
The barge has shallow water and "overfishing" penalties, so it's best to use this barge on long routes into oceans. The yield is maximised when the water is 20 or more blocks deep. Overfishing is tracked per barge and per block, routes 200 or more blocks long should be sufficient to avoid overfishing.

<video src="https://user-images.githubusercontent.com/31002977/149857342-05ef5100-05de-4899-b92f-ddec2d7ccfaf.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/149857342-05ef5100-05de-4899-b92f-ddec2d7ccfaf.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>

#### Chunk Loader Barge

This barge keeps 3x3 area of chunks, centered on the entity, loaded. Recommended for long-distance routes.

<img src="https://user-images.githubusercontent.com/31002977/150036818-47fc4cfd-8dc0-470f-9f26-d4b16a3e0f6c.png" style="max-height:35vh;">

#### Fluid Tank Barge

Use this barge to transport fluids, can be loaded and unloaded with a Fluid Hoppers.

### Chaining vessels

Used to link vessels together. Right click vessels with a vehicle chain in order to create a "vessel train". A train can only have one tugboat and cannot have loops.

<video src="https://user-images.githubusercontent.com/31002977/149873611-dcc6af47-7d9d-4117-927e-3ee7216faae9.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/149873611-dcc6af47-7d9d-4117-927e-3ee7216faae9.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>


### Tug Guide rail

Place under a block of water, works similar to vanilla powered rail. Can be used to force the tug to take a specific route. Does not affect barges. Shift-right-click to rotate.

<video src="https://user-images.githubusercontent.com/31002977/149873601-8dc6ea2b-f5d9-4cc7-992c-b40601eb093d.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/149873601-8dc6ea2b-f5d9-4cc7-992c-b40601eb093d.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>

### Vessel Corner Guide Rail

Assists vessels around sharp corners, should always be used on sharp corners or near entrances to narrow waterways (such as those for docks). Affects both tugs and barges. Shift-right-click to flip direction. Must not have a full solid block directly on top (i.e. no stone, planks etc. fence posts, signs etc. OK).

<video src="https://user-images.githubusercontent.com/31002977/161363613-bb064b66-32d8-48b8-9918-91e8eb92d6d2.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/161363613-bb064b66-32d8-48b8-9918-91e8eb92d6d2.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>

### Docking Vessels

There are two dock blocks: barge and tug. The tug block must always be present for the barge docks to work. The top texture of the tug dock must be aligned with the barge docks, you can shift right-click the tug dock to flip the dock direction. The barge docks must form a straight line from the tug dock, there shouldn't be any gaps in the line. The tug dock can only insert into the tug (orange/output mode), place a hopper on top of the dock to extract. The barge docks can both insert (orange/output mode) and extract (blue/input mode) into/from the barges, shift-right click to switch the mode. Place a hopper below the waterblock next to the barge dock to extract. The dock line should be next to a 1 block wide canal, wider canals can prevent the vessels from docking properly.

<video src="https://user-images.githubusercontent.com/31002977/161363642-3209e435-a297-4f91-9b9c-efd76e22858b.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/161363642-3209e435-a297-4f91-9b9c-efd76e22858b.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>

## Trains (Minecraft 1.18.2+, Little Logistics 1.2+)
Little logistics trains operate on vanilla rails, but use custom train car entities instead of minecarts.

### Locomotives

Locomotives are similar to tugs, can be used to pull train cars. You should always have exactly one locomotive in a train.

#### Steam Locomotive

Operates on any furnace fuel, can be loaded with hoppers. 

<img src="https://user-images.githubusercontent.com/31002977/161364757-43ade5aa-5b68-4887-847f-ca245659c097.png" style="max-height:35vh;">

#### Energy Locomotive

Uses Forge Energy from any mod, can be charged using Vehicle Charger.

<img src="https://user-images.githubusercontent.com/31002977/161364755-4bff9f40-6469-4c3d-8b5d-a719518d06b3.png" style="max-height:35vh;">

### Train cars

Train cars are similar to vanilla minecarts, but can be linked to a locomotive and docked.

#### Standard train car

Same as vanilla minecarts, can seat one player or a mob

<img src="https://user-images.githubusercontent.com/31002977/161364792-2c2fb6e9-1b36-44b1-a901-aaef658818f4.png" style="max-height:35vh;">

#### Chunk loader train car

Loads a 3x3 chunk area on the go, speed limits are enforced to avoid server lag. 

#### Chest train car

Same as vanilla chest minecart, can be loaded or unloaded using vanilla hoppers or rapid hoppers from this mod.

#### Fluid tank train car

Can transport fluids, can be loaded or unloaded using fluid hoppers. 

<img src="https://user-images.githubusercontent.com/31002977/161364752-6eddd6b7-189f-4d4e-bd15-5e16130c4bee.png" style="max-height:35vh;">

### Train linking

Trains can be linked using "vehicle chains". Train orientation will be updated automatically.

<video src="https://user-images.githubusercontent.com/31002977/161364954-cadc86f3-3f6b-4017-9396-12a9e387adc3.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/161364954-cadc86f3-3f6b-4017-9396-12a9e387adc3.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>

### Train Docking

Trains can be docked using a combination of "locomotive docking rails" and "train car docking rails". Line the docking rails up like shown on the video, place unloaders below the rails and loaders on either side. Blue mode is used to unload and orange to load. The rails will automatically tell the locomotive to wait as long as there's anything to be moved, if a redstone signal is applied to the locomotive rail, the locomotive will keep waiting regardless.

<video src="https://user-images.githubusercontent.com/31002977/161363736-800f3b40-dd7c-49c9-98b1-3c16ba660063.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/161363736-800f3b40-dd7c-49c9-98b1-3c16ba660063.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>

### Redstone-controlled junction rails

Standard Switch and T-junction tracks can be switched using redstone. Powered rails work great when trying to make loops.

<video src="https://user-images.githubusercontent.com/31002977/161364400-2ff1b9d3-bf43-4317-9e8d-3594ac5221af.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/161364400-2ff1b9d3-bf43-4317-9e8d-3594ac5221af.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>

### Automatic junction rails and routing

Automatic switch tracks will be switched by a locomotive with a route. Every time a locomotive approaches an automatic junction, it will look for the closest waypoint it is yet to visit, and switch the track accordingly. Waypoint can be configured using the "locomotive route" item, they are not ordered.

<video src="https://user-images.githubusercontent.com/31002977/161399628-d34fe302-6bf4-4845-9ffd-3bb85e710d55.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/161399628-d34fe302-6bf4-4845-9ffd-3bb85e710d55.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>

### Locomotive collision avoidance  

Locomotives have built-in collision detection that is designed for junctions. Locomotives AI will automatically decide and give priority to avoid a collision. 

<video src="https://user-images.githubusercontent.com/31002977/161364949-c6d1440a-4013-4d16-a6b1-fbd2482a75f0.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/161364949-c6d1440a-4013-4d16-a6b1-fbd2482a75f0.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video><br/>

### Locomotive speed

Locomotives will automatically control their speed on rails. They will travel faster on long stretches of straight tracks, but slow down as they approach docks, junctions, or corners. 

## Interfacing with vehicles

To compliment vanilla hoppers, this mod adds new blocks to load, unload, and interface with your vehicles. 

### Rapid hopper

Same as vanilla hopper, but 8 times as fast!

<img src="https://user-images.githubusercontent.com/31002977/161363836-315ae854-de67-405a-ae4e-971c09859c06.png" style="max-height:35vh;">

### Vehicle Charger

A charger for Energy Tug or Locomotive, connect to any forge energy cable to add power to the charger. Place on top of a dock or beside a rail to charge the vehicle when docking.

<img src="https://user-images.githubusercontent.com/31002977/161363667-f74d6fb7-8fdb-40b5-9be7-f59017e08c38.png" style="max-height:35vh;">

### Fluid Hopper

A hopper but for fluids, can import from the top and export from the sides. Can be loaded by right-clicking with buckets. Can be used to load/unload vessels using Docks.

<img src="https://user-images.githubusercontent.com/31002977/161363814-8cc93a44-8bfc-410d-826c-0b5bd6442625.png" style="max-height:35vh;">

### Vehicle Detector

This block emits a redstone signal from the back when it detects a vehicle, will display range when right-clicked.

<img src="https://user-images.githubusercontent.com/31002977/161403804-471698da-1b11-42b4-a0df-49463690a4aa.png" style="max-height:35vh;">
