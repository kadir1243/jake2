/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *  
 */
// Created on 07.01.2000 by RST.

package jake2.server;

import jake2.qcommon.*;
import jake2.qcommon.side.EnvType;
import jake2.qcommon.side.Environment;
import jake2.qcommon.util.Math3D;

@Environment(EnvType.SERVER)
class SV_WORLD {

    //ENTITY AREA CHECKING
    // World related
    //FIXME: this use of "area" is different from the bsp file use
    areanode_t[] sv_areanodes;
    int sv_numareanodes;
    float[] area_mins;
    float[] area_maxs;
    edict_t[] area_list;
    int area_count;
    int area_maxcount;
    int area_type;
    edict_t[] touch = new edict_t[Defines.MAX_EDICTS];
    edict_t[] touchlist = new edict_t[Defines.MAX_EDICTS];
    public final int MAX_TOTAL_ENT_LEAFS = 128;
    int[] leafs = new int[MAX_TOTAL_ENT_LEAFS];
    int[] clusters = new int[MAX_TOTAL_ENT_LEAFS];

    /**
     * SV_WORLD.initNodes()
     */
    void clearAreaNodes() {
        sv_numareanodes = 0;
        sv_areanodes = new areanode_t[Defines.AREA_NODES];
        for (int n = 0; n < Defines.AREA_NODES; n++)
            sv_areanodes[n] = new areanode_t();
    }

    // ClearLink is used for new headnodes
    private static void ClearLink(link_t l) {
        l.prev = l.next = l;
    }

    private static void RemoveLink(link_t l) {
        l.next.prev = l.prev;
        l.prev.next = l.next;
    }

    private static void InsertLinkBefore(link_t l, link_t before) {
        l.next = before;
        l.prev = before.prev;
        l.prev.next = l;
        l.next.prev = l;
    }

    /*
     * =============== SV_CreateAreaNode
     * 
     * Builds a uniformly subdivided tree for the given world size
     * ===============
     */
    private static areanode_t SV_CreateAreaNode(int depth, float[] mins,
                                                float[] maxs, GameImportsImpl gameImports) {
        areanode_t anode;
        float[] size = { 0, 0, 0 };
        float[] mins1 = { 0, 0, 0 }, maxs1 = { 0, 0, 0 }, mins2 = { 0, 0, 0 }, maxs2 = {
                0, 0, 0 };
        anode = gameImports.world.sv_areanodes[gameImports.world.sv_numareanodes];
        // just for debugging (rst)
//        Math3D.VectorCopy(mins, anode.mins_rst);
//        Math3D.VectorCopy(maxs, anode.maxs_rst);
        gameImports.world.sv_numareanodes++;
        ClearLink(anode.trigger_edicts);
        ClearLink(anode.solid_edicts);
        if (depth == Defines.AREA_DEPTH) {
            anode.axis = -1;
            anode.children[0] = anode.children[1] = null;
            return anode;
        }
        Math3D.VectorSubtract(maxs, mins, size);
        if (size[0] > size[1])
            anode.axis = 0;
        else
            anode.axis = 1;
        anode.dist = 0.5f * (maxs[anode.axis] + mins[anode.axis]);
        Math3D.VectorCopy(mins, mins1);
        Math3D.VectorCopy(mins, mins2);
        Math3D.VectorCopy(maxs, maxs1);
        Math3D.VectorCopy(maxs, maxs2);
        maxs1[anode.axis] = mins2[anode.axis] = anode.dist;
        anode.children[0] = SV_CreateAreaNode(depth + 1, mins2, maxs2, gameImports);
        anode.children[1] = SV_CreateAreaNode(depth + 1, mins1, maxs1, gameImports);
        return anode;
    }

    /*
     * =============== SV_ClearWorld
     * 
     * ===============
     */
    static void SV_ClearWorld(GameImportsImpl gameImports) {
        gameImports.world.clearAreaNodes();
        SV_CreateAreaNode(0, gameImports.sv.models[1].mins, gameImports.sv.models[1].maxs, gameImports);
        /*
         * Com.p("areanodes:" + sv_numareanodes + " (sollten 32 sein)."); for
         * (int n = 0; n < sv_numareanodes; n++) { Com.Printf( "|%3i|%2i|%8.2f
         * |%8.2f|%8.2f|%8.2f| %8.2f|%8.2f|%8.2f|\n", new Vargs() .add(n)
         * .add(sv_areanodes[n].axis) .add(sv_areanodes[n].dist)
         * .add(sv_areanodes[n].mins_rst[0]) .add(sv_areanodes[n].mins_rst[1])
         * .add(sv_areanodes[n].mins_rst[2]) .add(sv_areanodes[n].maxs_rst[0])
         * .add(sv_areanodes[n].maxs_rst[1]) .add(sv_areanodes[n].maxs_rst[2])); }
         */
    }

    /*
     * =============== SV_UnlinkEdict ===============
     */
    static void SV_UnlinkEdict(edict_t ent) {
        if (null == ent.area.prev)
            return; // not linked in anywhere
        RemoveLink(ent.area);
        ent.area.prev = ent.area.next = null;
    }

    static void SV_LinkEdict(edict_t ent, GameImportsImpl gameImports) {
        areanode_t node;
        int num_leafs;
        int j, k;
        int area;
        int topnode = 0;
        if (ent.area.prev != null)
            SV_UnlinkEdict(ent); // unlink from old position
        if (ent == gameImports.gameExports.getEdict(0))
            return; // don't add the world
        if (!ent.inuse)
            return;
        // set the size
        Math3D.VectorSubtract(ent.maxs, ent.mins, ent.size);
        // encode the size into the entity_state for client prediction
        if (ent.solid == Defines.SOLID_BBOX
                && 0 == (ent.svflags & Defines.SVF_DEADMONSTER)) {
            // assume that x/y are equal and symetric
            int i = (int) (ent.maxs[0] / 8);
            if (i < 1)
                i = 1;
            if (i > 31)
                i = 31;
            // z is not symetric
            j = (int) ((-ent.mins[2]) / 8);
            if (j < 1)
                j = 1;
            if (j > 31)
                j = 31;
            // and z maxs can be negative...
            k = (int) ((ent.maxs[2] + 32) / 8);
            if (k < 1)
                k = 1;
            if (k > 63)
                k = 63;
            ent.s.solid = (k << 10) | (j << 5) | i;
        } else if (ent.solid == Defines.SOLID_BSP) {
            ent.s.solid = 31; // a solid_bbox will never create this value
        } else
            ent.s.solid = 0;
        // set the abs box
        if (ent.solid == Defines.SOLID_BSP
                && (ent.s.angles[0] != 0 || ent.s.angles[1] != 0 || ent.s.angles[2] != 0)) {
            // expand for rotation
            float max, v;
            max = 0;
            for (int i = 0; i < 3; i++) {
                v = Math.abs(ent.mins[i]);
                if (v > max)
                    max = v;
                v = Math.abs(ent.maxs[i]);
                if (v > max)
                    max = v;
            }
            for (int i = 0; i < 3; i++) {
                ent.absmin[i] = ent.s.origin[i] - max;
                ent.absmax[i] = ent.s.origin[i] + max;
            }
        } else {
            // normal
            Math3D.VectorAdd(ent.s.origin, ent.mins, ent.absmin);
            Math3D.VectorAdd(ent.s.origin, ent.maxs, ent.absmax);
        }
        // because movement is clipped an epsilon away from an actual edge,
        // we must fully check even when bounding boxes don't quite touch
        ent.absmin[0]--;
        ent.absmin[1]--;
        ent.absmin[2]--;
        ent.absmax[0]++;
        ent.absmax[1]++;
        ent.absmax[2]++;
        // link to PVS leafs
        ent.num_clusters = 0;
        ent.areanum = 0;
        ent.areanum2 = 0;
        // get all leafs, including solids
        int iw[] = { topnode };
        num_leafs = gameImports.cm.CM_BoxLeafnums(ent.absmin, ent.absmax, gameImports.world.leafs, gameImports.world.MAX_TOTAL_ENT_LEAFS, iw);
        topnode = iw[0];
        // set areas
        for (int i = 0; i < num_leafs; i++) {
            gameImports.world.clusters[i] = gameImports.cm.CM_LeafCluster(gameImports.world.leafs[i]);
            area = gameImports.cm.CM_LeafArea(gameImports.world.leafs[i]);
            if (area != 0) {
                // doors may legally straggle two areas,
                // but nothing should ever need more than that
                if (ent.areanum != 0 && ent.areanum != area) {
                    if (ent.areanum2 != 0 && ent.areanum2 != area
                            && gameImports.sv.state == ServerStates.SS_LOADING)
                        Com.DPrintf("Object touching 3 areas at "
                                + ent.absmin[0] + " " + ent.absmin[1] + " "
                                + ent.absmin[2] + "\n");
                    ent.areanum2 = area;
                } else
                    ent.areanum = area;
            }
        }
        if (num_leafs >= gameImports.world.MAX_TOTAL_ENT_LEAFS) {
            // assume we missed some leafs, and mark by headnode
            ent.num_clusters = -1;
            ent.headnode = topnode;
        } else {
            ent.num_clusters = 0;
            for (int i = 0; i < num_leafs; i++) {
                if (gameImports.world.clusters[i] == -1)
                    continue; // not a visible leaf
                for (j = 0; j < i; j++)
                    if (gameImports.world.clusters[j] == gameImports.world.clusters[i])
                        break;
                if (j == i) {
                    if (ent.num_clusters == Defines.MAX_ENT_CLUSTERS) {
                        // assume we missed some leafs, and mark by headnode
                        ent.num_clusters = -1;
                        ent.headnode = topnode;
                        break;
                    }
                    ent.clusternums[ent.num_clusters++] = gameImports.world.clusters[i];
                }
            }
        }
        // if first time, make sure old_origin is valid
        if (0 == ent.linkcount) {
            Math3D.VectorCopy(ent.s.origin, ent.s.old_origin);
        }
        ent.linkcount++;
        if (ent.solid == Defines.SOLID_NOT)
            return;
        // find the first node that the ent's box crosses
        node = gameImports.world.sv_areanodes[0];
        while (true) {
            if (node.axis == -1)
                break;
            if (ent.absmin[node.axis] > node.dist)
                node = node.children[0];
            else if (ent.absmax[node.axis] < node.dist)
                node = node.children[1];
            else
                break; // crosses the node
        }
        // link it in
        if (ent.solid == Defines.SOLID_TRIGGER)
            InsertLinkBefore(ent.area, node.trigger_edicts);
        else
            InsertLinkBefore(ent.area, node.solid_edicts);
    }

    /*
     * ==================== SV_AreaEdicts_r
     * 
     * ====================
     */
    private static void SV_AreaEdicts_r(areanode_t node, GameImportsImpl gameImports) {
        link_t l, next, start;
        edict_t check;
        // touch linked edicts
        if (gameImports.world.area_type == Defines.AREA_SOLID)
            start = node.solid_edicts;
        else
            start = node.trigger_edicts;
        for (l = start.next; l != start; l = next) {
            next = l.next;
            check = (edict_t) l.o;
            if (check.solid == Defines.SOLID_NOT)
                continue; // deactivated
            if (check.absmin[0] > gameImports.world.area_maxs[0]
                    || check.absmin[1] > gameImports.world.area_maxs[1]
                    || check.absmin[2] > gameImports.world.area_maxs[2]
                    || check.absmax[0] < gameImports.world.area_mins[0]
                    || check.absmax[1] < gameImports.world.area_mins[1]
                    || check.absmax[2] < gameImports.world.area_mins[2])
                continue; // not touching
            if (gameImports.world.area_count == gameImports.world.area_maxcount) {
                Com.Printf("SV_AreaEdicts: MAXCOUNT\n");
                return;
            }
            gameImports.world.area_list[gameImports.world.area_count] = check;
            gameImports.world.area_count++;
        }
        if (node.axis == -1)
            return; // terminal node
        // recurse down both sides
        if (gameImports.world.area_maxs[node.axis] > node.dist)
            SV_AreaEdicts_r(node.children[0], gameImports);
        if (gameImports.world.area_mins[node.axis] < node.dist)
            SV_AreaEdicts_r(node.children[1], gameImports);
    }

    /*
     * ================ SV_AreaEdicts ================
     * TODO: return a collection of edicts rather than modifying param
     */
    static int SV_AreaEdicts(float[] mins, float[] maxs, edict_t[] list, int maxcount, int areatype, GameImportsImpl gameImports) {
        gameImports.world.area_mins = mins;
        gameImports.world.area_maxs = maxs;
        gameImports.world.area_list = list;
        gameImports.world.area_count = 0;
        gameImports.world.area_maxcount = maxcount;
        gameImports.world.area_type = areatype;
        SV_AreaEdicts_r(gameImports.world.sv_areanodes[0], gameImports);
        return gameImports.world.area_count;
    }

    /*
     * ============= SV_PointContents =============
     */
    static int SV_PointContents(float[] p, GameImportsImpl gameImports) {
        edict_t hit;
        int i, num;
        int contents, c2;
        int headnode;
        // get base contents from world
        contents = gameImports.cm.PointContents(p, gameImports.sv.models[1].headnode);
        // or in contents from all the other entities
        num = SV_AreaEdicts(p, p, gameImports.world.touch, Defines.MAX_EDICTS, Defines.AREA_SOLID, gameImports);
        for (i = 0; i < num; i++) {
            hit = gameImports.world.touch[i];
            // might intersect, so do an exact clip
            headnode = SV_HullForEntity(hit, gameImports);
            if (hit.solid != Defines.SOLID_BSP) {
	    }
            c2 = gameImports.cm.TransformedPointContents(p, headnode, hit.s.origin,
                    hit.s.angles);
            contents |= c2;
        }
        return contents;
    }

    /*
     * ================ SV_HullForEntity
     * 
     * Returns a headnode that can be used for testing or clipping an object of
     * mins/maxs size. Offset is filled in to contain the adjustment that must
     * be added to the testing object's origin to get a point to use with the
     * returned hull. ================
     */
    private static int SV_HullForEntity(edict_t ent, GameImportsImpl gameImports) {
        cmodel_t model;
        // decide which clipping hull to use, based on the size
        if (ent.solid == Defines.SOLID_BSP) {
            // explicit hulls in the BSP model
            model = gameImports.sv.models[ent.s.modelindex];
            if (null == model)
                Com.Error(Defines.ERR_FATAL,
                        "MOVETYPE_PUSH with a non bsp model");
            return model.headnode;
        }
        // create a temp hull from bounding box sizes
        return gameImports.cm.HeadnodeForBox(ent.mins, ent.maxs);
    }

    private static void SV_ClipMoveToEntities(moveclip_t clip, GameImportsImpl gameImports) {
        int i, num;
        edict_t touch;
        trace_t trace;
        int headnode;
        float angles[];
        num = SV_AreaEdicts(clip.boxmins, clip.boxmaxs, gameImports.world.touchlist, Defines.MAX_EDICTS, Defines.AREA_SOLID, gameImports);
        // be careful, it is possible to have an entity in this
        // list removed before we get to it (killtriggered)
        for (i = 0; i < num; i++) {
            touch = gameImports.world.touchlist[i];
            if (touch.solid == Defines.SOLID_NOT)
                continue;
            if (touch == clip.passedict)
                continue;
            if (clip.trace.allsolid)
                return;
            if (clip.passedict != null) {
                if (touch.getOwner() == clip.passedict)
                    continue; // don't clip against own missiles
                if (clip.passedict.getOwner() == touch)
                    continue; // don't clip against owner
            }
            if (0 == (clip.contentmask & Defines.CONTENTS_DEADMONSTER)
                    && 0 != (touch.svflags & Defines.SVF_DEADMONSTER))
                continue;
            // might intersect, so do an exact clip
            headnode = SV_HullForEntity(touch, gameImports);
            angles = touch.s.angles;
            if (touch.solid != Defines.SOLID_BSP)
                angles = Globals.vec3_origin; // boxes don't rotate
            if ((touch.svflags & Defines.SVF_MONSTER) != 0)
                trace = gameImports.cm.TransformedBoxTrace(clip.start, clip.end,
                        clip.mins2, clip.maxs2, headnode, clip.contentmask,
                        touch.s.origin, angles);
            else
                trace = gameImports.cm.TransformedBoxTrace(clip.start, clip.end, clip.mins,
                        clip.maxs, headnode, clip.contentmask, touch.s.origin,
                        angles);
            if (trace.allsolid || trace.startsolid
                    || trace.fraction < clip.trace.fraction) {
                trace.ent = touch;
                if (clip.trace.startsolid) {
                    clip.trace = trace;
                    clip.trace.startsolid = true;
                } else
                    clip.trace.set(trace);
            } else if (trace.startsolid)
                clip.trace.startsolid = true;
        }
    }

    /*
     * ================== SV_TraceBounds ==================
     */
    private static void SV_TraceBounds(float[] start, float[] mins,
                                       float[] maxs, float[] end, float[] boxmins, float[] boxmaxs) {
        int i;
        for (i = 0; i < 3; i++) {
            if (end[i] > start[i]) {
                boxmins[i] = start[i] + mins[i] - 1;
                boxmaxs[i] = end[i] + maxs[i] + 1;
            } else {
                boxmins[i] = end[i] + mins[i] - 1;
                boxmaxs[i] = start[i] + maxs[i] + 1;
            }
        }
    }

    /*
     * ================== SV_Trace
     * 
     * Moves the given mins/maxs volume through the world from start to end.
     * 
     * Passedict and edicts owned by passedict are explicitly not checked.
     * 
     * ==================
     */
    static trace_t SV_Trace(float[] start, float[] mins, float[] maxs, float[] end, edict_t passedict, int contentmask, GameImportsImpl gameImports) {
        moveclip_t clip = new moveclip_t();
        if (mins == null)
            mins = Globals.vec3_origin;
        if (maxs == null)
            maxs = Globals.vec3_origin;

        // clip to world
        clip.trace = gameImports.cm.BoxTrace(start, end, mins, maxs, 0, contentmask);
        clip.trace.ent = gameImports.gameExports.getEdict(0);
        if (clip.trace.fraction == 0)
            return clip.trace; // blocked by the world
        clip.contentmask = contentmask;
        clip.start = start;
        clip.end = end;
        clip.mins = mins;
        clip.maxs = maxs;
        clip.passedict = passedict;
        Math3D.VectorCopy(mins, clip.mins2);
        Math3D.VectorCopy(maxs, clip.maxs2);
        // create the bounding box of the entire move
        SV_TraceBounds(start, clip.mins2, clip.maxs2, end, clip.boxmins,
                clip.boxmaxs);
        // clip to other solid entities
        SV_ClipMoveToEntities(clip, gameImports);
        return clip.trace;
    }
}