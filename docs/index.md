# Overview and Guide

Little Logistics is a mod for Minecraft Forge focused on efficient, affordable, long-distance transport. This guide does not cover recepies, to check item recepies use JEI or a similar mod. 

## Vessels

Vessels are the core of this mod. There are two types of vessels: Barges (that can do all sorts of useful things), and Tugs (that drag the barges). If vessels get stuck, they can be moved slightly using fishing rods (just like other entities). Tugs can also be dragged with leads, right click on the hook at the back of the boat to attach. 

### Steam Tug

A basic tugboat that can operate on any furnace fuel. Fuel can be loaded with hoppers placed on top of tug docks. 

<img src="https://user-images.githubusercontent.com/31002977/150037890-3bd23e8c-8aea-4910-85c3-34ba49c7504c.png" style="max-height:35vh;">

### Energy Tug 

_COMING SOON_

### Chest Barge

A simple barge that holds items, very similar to vanilla minecart. Must be docked and part of a tug-train to interface with hoppers.

<img src="https://user-images.githubusercontent.com/31002977/150036819-99629a2b-d3db-4058-aa16-4bd7bed7b1ac.png" style="max-height:35vh;">


### Auto-Fishing Barge

Performs fishing passively, uses vanilla fishing rod loot table. Needs to be in open water for nets to be deployed. The barge has shallow water and "overfishing" penalties so it's best to use this barge on long routes into oceans. Can be unloaded with hoppers and barge docks. Must be docked and part of a tug-train to interface with hoppers.

<video src="https://user-images.githubusercontent.com/31002977/149857342-05ef5100-05de-4899-b92f-ddec2d7ccfaf.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/149857342-05ef5100-05de-4899-b92f-ddec2d7ccfaf.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video>

### Chunk Loader Barge

This barge keeps 3x3 area of chunks, centered on the enitity, loaded. Recommended for long-distance routes. 

<img src="https://user-images.githubusercontent.com/31002977/150036818-47fc4cfd-8dc0-470f-9f26-d4b16a3e0f6c.png" style="max-height:35vh;">

### Fluid Storage Barge

_COMING SOON_

## Items

### Vessel Chain

Used to link vessels together. Right click vessels in order to create a "vessel train". A train can only have one tugboat and cannot have loops. 

<video src="https://user-images.githubusercontent.com/31002977/149873611-dcc6af47-7d9d-4117-927e-3ee7216faae9.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/149873611-dcc6af47-7d9d-4117-927e-3ee7216faae9.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video>

### Tug route 

This item stores a route for the tug. When the item is placed in the tug's route slot, the tug will follow the route in order from node 0. Right click at any coordinate to add a node to the **back** of the tug route (cannot add to the middle). Right click at the location of any existing node to remove it.

<video src="https://user-images.githubusercontent.com/31002977/149873867-d83fec24-2a14-4774-b3f3-feaa08d27440.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/149873867-d83fec24-2a14-4774-b3f3-feaa08d27440.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video>

## Blocks

There are two types of blocks: docks and guide rails. Docks can be used to load/unload vessels; guide rails should be used around narrow waterways, to prevent the vessels from getting stuck. 

### Tug Guide rail

Place under a block of water, works similar to vanilla powered rail. Can be used to force the tug to take a specific route. Does not affect barges. Shift-right-click to rotate.

<video src="https://user-images.githubusercontent.com/31002977/149873601-8dc6ea2b-f5d9-4cc7-992c-b40601eb093d.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/149873601-8dc6ea2b-f5d9-4cc7-992c-b40601eb093d.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video>

### Vessel Corner Guide Rail

Assits vessels around sharp corners, should always be used on sharp corners or near entrances to narrow waterways (such as those for docks). Affects both tugs and barges. Shift-right-click to flip direction. Must not have a full solid block directilly on top (i.e. no stone, planks etc. fence posts, signs etc OK). 

<video src="https://user-images.githubusercontent.com/31002977/149873858-95ea9970-2b91-4cd0-a7ba-9a2d2d545292.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/149873858-95ea9970-2b91-4cd0-a7ba-9a2d2d545292.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video>

### Docks

There are two dock blogs: barge and tug. The tug block must always be present for the barge docks to work. The top texture of the tug dock must be alligned with the barge docks, you can shift right-click the tug dock to flip the dock direction. The barge docks must form a straight line from the tug dock, there shouldn't be any gaps in the line. The tug dock can only insert into the tug (orange/output mode), place a hopper on top of the dock to extract. The barge docks can both insert (orange/output mode) and extract (blue/input) mode into/from the barges, shift-right click to switch the mode. Place a hopper below the waterblock next to the barge dock to extract. The dock line be next to a 1 block wide canal, wider canals can prevent the vessels from docking properly.


<video src="https://user-images.githubusercontent.com/31002977/149874251-ccc26b7d-b74c-474b-b066-e48a01872000.mp4" data-canonical-src="https://user-images.githubusercontent.com/31002977/149874251-ccc26b7d-b74c-474b-b066-e48a01872000.mp4" controls="controls" muted="muted" class="d-block rounded-bottom-2 width-fit" style="max-height:35vh;"></video>

## License

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

