package com.openrsc.server.plugins;

import com.openrsc.server.constants.IronmanMode;
import com.openrsc.server.constants.ItemId;
import com.openrsc.server.constants.Quests;
import com.openrsc.server.constants.Skills;
import com.openrsc.server.event.SingleEvent;
import com.openrsc.server.event.custom.UndergroundPassMessages;
import com.openrsc.server.event.rsc.PluginTask;
import com.openrsc.server.external.GameObjectLoc;
import com.openrsc.server.login.BankPinChangeRequest;
import com.openrsc.server.login.BankPinVerifyRequest;
import com.openrsc.server.model.MenuOptionListener;
import com.openrsc.server.model.Path;
import com.openrsc.server.model.Path.PathType;
import com.openrsc.server.model.Point;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.GroundItem;
import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.entity.npc.Npc;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.entity.update.Bubble;
import com.openrsc.server.model.entity.update.BubbleNpc;
import com.openrsc.server.model.entity.update.ChatMessage;
import com.openrsc.server.model.world.World;
import com.openrsc.server.model.world.region.TileValue;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.Formulae;
import com.openrsc.server.util.rsc.MessageType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class Functions {

	private static String getBankPinInput(Player player) {
		ActionSender.sendBankPinInterface(player);
		player.setAttribute("bank_pin_entered", "");
		String enteredPin = null;
		while (true) {
			enteredPin = player.getAttribute("bank_pin_entered", "");
			if (enteredPin != "") {
				break;
			}
			Functions.sleep(640);
		}
		if (enteredPin.equals("cancel")) {
			ActionSender.sendCloseBankPinInterface(player);
			return null;
		}
		return enteredPin;
	}

	public static boolean removeBankPin(final Player player) {
		BankPinChangeRequest request;
		String oldPin;

		if(!player.getCache().hasKey("bank_pin")) {
			player.playerServerMessage(MessageType.QUEST, "You do not have a bank pin to remove");
			return false;
		}

		oldPin = getBankPinInput(player);

		if(oldPin == null) {
			player.playerServerMessage(MessageType.QUEST, "You have cancelled removing your bank pin");
			return false;
		}

		request = new BankPinChangeRequest(player.getWorld().getServer(), player, oldPin, null);
		player.getWorld().getServer().getLoginExecutor().add(request);

		while(!request.isProcessed()) {
			Functions.sleep(640);
		}

		return true;
	}

	public static boolean setBankPin(final Player player) {
		BankPinChangeRequest request;
		String newPin;

		if(player.getCache().hasKey("bank_pin")) {
			player.playerServerMessage(MessageType.QUEST, "You already have a bank pin");
			return false;
		}

		newPin = getBankPinInput(player);

		if(newPin == null) {
			player.playerServerMessage(MessageType.QUEST, "You have cancelled creating your bank pin");
			return false;
		}

		request = new BankPinChangeRequest(player.getWorld().getServer(), player, null, newPin);
		player.getWorld().getServer().getLoginExecutor().add(request);

		while(!request.isProcessed()) {
			Functions.sleep(640);
		}

		return true;
	}

	public static boolean changeBankPin(final Player player) {
		BankPinChangeRequest request;
		String newPin;
		String oldPin;

		if(!player.getCache().hasKey("bank_pin")) {
			player.playerServerMessage(MessageType.QUEST, "You do not have a bank pin to change");
			return false;
		}

		oldPin = getBankPinInput(player);

		if(oldPin == null) {
			player.playerServerMessage(MessageType.QUEST, "You have cancelled changing your bankpin");
			return false;
		}

		newPin = getBankPinInput(player);

		if(newPin == null) {
			player.playerServerMessage(MessageType.QUEST, "You have cancelled changing your bankpin");
			return false;
		}

		request = new BankPinChangeRequest(player.getWorld().getServer(), player, oldPin, newPin);
		player.getWorld().getServer().getLoginExecutor().add(request);

		while(!request.isProcessed()) {
			Functions.sleep(640);
		}

		return true;
	}

	public static boolean validateBankPin(final Player player) {
		BankPinVerifyRequest request;
		String pin;

		if (!player.getWorld().getServer().getConfig().WANT_BANK_PINS) {
			return true;
		}

		if(!player.getCache().hasKey("bank_pin")) {
			player.setAttribute("bankpin", true);
			return true;
		}

		if(player.getAttribute("bankpin", false)) {
			return true;
		}

		pin = getBankPinInput(player);

		request = new BankPinVerifyRequest(player.getWorld().getServer(), player, pin);
		player.getWorld().getServer().getLoginExecutor().add(request);

		while(!request.isProcessed()) {
			Functions.sleep(640);
		}

		return player.getAttribute("bankpin", false);
	}

	public static int getWoodcutAxe(Player p) {
		int axeId = -1;

		for (final int a : Formulae.woodcuttingAxeIDs) {
			if (p.getWorld().getServer().getConfig().WANT_EQUIPMENT_TAB) {
				if (p.getEquipment().hasEquipped(a) != -1) {
					axeId = a;
					break;
				}
			}

			if (p.getInventory().countId(a) > 0) {
				axeId = a;
				break;
			}
		}
		return axeId;
	}

	public static void teleport(Mob n, int x, int y) {
		n.getWorld().getServer().getGameEventHandler().submit(() -> {
			n.resetPath();
			n.setLocation(new Point(x, y), true);
		}, "Teleport");
	}

	public static void walkMob(Mob n, Point... waypoints) {
		n.getWorld().getServer().getGameEventHandler().submit(() -> {
			n.resetPath();
			Path path = new Path(n, PathType.WALK_TO_POINT);
			for (Point p : waypoints) {
				path.addStep(p.getX(), p.getY());
			}
			path.finish();
			n.getWalkingQueue().setPath(path);
		}, "Walk Mob");
	}

	public static void walkThenTeleport(final Player player, final int x1, final int y1, final int x2, final int y2, final boolean bubble) {
		player.walk(x1, y1);
		while (!player.getWalkingQueue().finished()) {
			sleep(1);
		}
		player.teleport(x2, y2, bubble);
	}

	public static boolean hasItemAtAll(Player p, int id) {
		return p.getBank().contains(new Item(id)) || p.getInventory().contains(new Item(id));
	}

	public static boolean isWielding(Player p, int i) {
		return p.getInventory().wielding(i);
	}

	public static boolean inArray(Object o, Object... oArray) {
		for (Object object : oArray) {
			if (o.equals(object) || o == object) {
				return true;
			}
		}
		return false;
	}

	public static boolean inArray(int o, int[] oArray) {
		for (int object : oArray) {
			if (o == object) {
				return true;
			}
		}
		return false;
	}

	public static void kill(Npc mob, Player killedBy) {
		mob.killedBy(killedBy);
	}

	/**
	 * Determines if the id of item1 is idA and the id of item2 is idB
	 * and does the check the other way around as well
	 */
	public static boolean compareItemsIds(Item item1, Item item2, int idA, int idB) {
		return item1.getID() == idA && item2.getID() == idB || item1.getID() == idB && item2.getID() == idA;
	}

	/**
	 * Returns true if you are in any stages provided.
	 *
	 * @param p
	 * @param quest
	 * @param stage
	 * @return
	 */
	public static boolean atQuestStages(Player p, QuestInterface quest, int... stage) {
		boolean flag = false;
		for (int s : stage) {
			if (atQuestStage(p, quest, s)) {
				flag = true;
			}
		}
		return flag;
	}

	/**
	 * Returns true if you are in any stages provided.
	 *
	 * @param p
	 * @param qID
	 * @param stage
	 * @return
	 */
	public static boolean atQuestStages(Player p, int qID, int... stage) {
		boolean flag = false;
		for (int s : stage) {
			if (atQuestStage(p, qID, s)) {
				flag = true;
			}
		}
		return flag;
	}

	public static void attack(Npc npc, Player p) {
		npc.setChasing(p);
	}

	public static void attack(Npc npc, Npc npc2) {
		npc.setChasing(npc2);
	}

	public static int getCurrentLevel(Player p, int i) {
		return p.getSkills().getLevel(i);
	}

	public static int getMaxLevel(Player p, int i) {
		return p.getSkills().getMaxStat(i);
	}

	public static int getMaxLevel(Mob n, int i) {
		return n.getSkills().getMaxStat(i);
	}

	public static void setCurrentLevel(Player p, int skill, int level) {
		p.getSkills().setLevel(skill, level);
		ActionSender.sendStat(p, skill);
	}

	/**
	 * QuestData: Quest Points, Exp Skill ID, Base Exp, Variable Exp
	 *
	 * @param p         - the player
	 * @param questData - the data, if skill id is < 0 means no exp is applied
	 * @param applyQP   - apply the quest point increase
	 */
	public static void incQuestReward(Player p, int[] questData, boolean applyQP) {
		int qp = questData[0];
		int skillId = questData[1];
		int baseXP = questData[2];
		int varXP = questData[3];
		if (skillId >= 0 && baseXP > 0 && varXP >= 0) {
			p.incQuestExp(skillId, p.getSkills().getMaxStat(skillId) * varXP + baseXP);
		}
		if (applyQP) {
			p.incQuestPoints(qp);
		}
	}

	public static void movePlayer(Player p, int x, int y) {
		movePlayer(p, x, y, false);

	}

	public static void movePlayer(Player p, int x, int y, boolean worldInfo) {
		if (worldInfo)
			p.teleport(x, y, false);
		else
			p.teleport(x, y);

	}

	public static void displayTeleportBubble(Player p, int x, int y, boolean teleGrab) {
		for (Object o : p.getViewArea().getPlayersInView()) {
			Player pt = ((Player) o);
			if (teleGrab)
				ActionSender.sendTeleBubble(pt, x, y, true);
			else
				ActionSender.sendTeleBubble(pt, x, y, false);
		}
	}

	private static boolean checkBlocking(Npc npc, int x, int y, int bit) {
		TileValue t = npc.getWorld().getTile(x, y);
		Point p = new Point(x, y);
		for (Npc n : npc.getViewArea().getNpcsInView()) {
			if (n.getLocation().equals(p)) {
				return true;
			}
		}
		for (Player areaPlayer : npc.getViewArea().getPlayersInView()) {
			if (areaPlayer.getLocation().equals(p)) {
				return true;
			}
		}
		return isBlocking(t.traversalMask, (byte) bit);
	}

	private static boolean isBlocking(int objectValue, byte bit) {
		if ((objectValue & bit) != 0) { // There is a wall in the way
			return true;
		}
		if ((objectValue & 16) != 0) { // There is a diagonal wall here:
			// \
			return true;
		}
		if ((objectValue & 32) != 0) { // There is a diagonal wall here:
			// /
			return true;
		}
		if ((objectValue & 64) != 0) { // This tile is unwalkable
			return true;
		}
		return false;
	}

	public static Point canWalk(Npc n, int x, int y) {
		int myX = n.getX();
		int myY = n.getY();
		int newX = x;
		int newY = y;
		boolean myXBlocked = false, myYBlocked = false, newXBlocked = false, newYBlocked = false;
		if (myX > x) {
			myXBlocked = checkBlocking(n, myX - 1, myY, 8); // Check right
			// tiles
			newX = myX - 1;
		} else if (myX < x) {
			myXBlocked = checkBlocking(n, myX + 1, myY, 2); // Check left
			// tiles
			newX = myX + 1;
		}
		if (myY > y) {
			myYBlocked = checkBlocking(n, myX, myY - 1, 4); // Check top tiles
			newY = myY - 1;
		} else if (myY < y) {
			myYBlocked = checkBlocking(n, myX, myY + 1, 1); // Check bottom
			// tiles
			newY = myY + 1;
		}

		if ((myXBlocked && myYBlocked) || (myXBlocked && myY == newY) || (myYBlocked && myX == newX)) {
			return null;
		}

		if (newX > myX) {
			newXBlocked = checkBlocking(n, newX, newY, 2);
		} else if (newX < myX) {
			newXBlocked = checkBlocking(n, newX, newY, 8);
		}

		if (newY > myY) {
			newYBlocked = checkBlocking(n, newX, newY, 1);
		} else if (newY < myY) {
			newYBlocked = checkBlocking(n, newX, newY, 4);
		}
		if ((newXBlocked && newYBlocked) || (newXBlocked && myY == newY) || (myYBlocked && myX == newX)) {
			return null;
		}
		if ((myXBlocked && newXBlocked) || (myYBlocked && newYBlocked)) {
			return null;
		}
		return new Point(newX, newY);
	}

	public static void npcWalkFromPlayer(Player player, Npc n) {
		if (player.getLocation().equals(n.getLocation())) {
			for (int x = -1; x <= 1; ++x) {
				for (int y = -1; y <= 1; ++y) {
					if (x == 0 || y == 0)
						continue;
					Point destination = canWalk(n, player.getX() - x, player.getY() - y);
					if (destination != null && destination.inBounds(n.getLoc().minX, n.getLoc().minY, n.getLoc().maxY, n.getLoc().maxY)) {
						n.walk(destination.getX(), destination.getY());
						break;
					}
				}
			}
		}
	}

	public static Npc spawnNpc(int id, int x, int y, final int time, final Player spawnedFor) {
		final Npc npc = new Npc(spawnedFor.getWorld(), id, x, y);
		spawnedFor.getWorld().getServer().getGameEventHandler().submit(() -> {
			npc.setShouldRespawn(false);
			npc.setAttribute("spawnedFor", spawnedFor);
			spawnedFor.getWorld().registerNpc(npc);
			spawnedFor.getWorld().getServer().getGameEventHandler().add(new SingleEvent(spawnedFor.getWorld(), null, time, "Spawn Pet NPC Timed") {
				public void action() {
					npc.remove();
				}
			});
		}, "Spawn Pet NPC Delayed");
		return npc;
	}

	public static Npc spawnNpc(World world, int id, int x, int y) {
		final Npc npc = new Npc(world, id, x, y);
		world.getServer().getGameEventHandler().submit(() -> {
			npc.setShouldRespawn(false);
			world.registerNpc(npc);
		}, "Spawn Permanent NPC Delayed");
		return npc;
	}

	public static Npc spawnNpcWithRadius(Player p, int id, int x, int y, int radius, final int time) {

		final Npc npc = new Npc(p.getWorld(), id, x, y, radius);
		p.getWorld().getServer().getGameEventHandler().submit(() -> {
			npc.setShouldRespawn(false);
			p.getWorld().registerNpc(npc);
			p.getWorld().getServer().getGameEventHandler().add(new SingleEvent(p.getWorld(), null, time, "Spawn Radius NPC Timed") {
				public void action() {
					npc.remove();
				}
			});
		}, "Spawn Radius NPC Delayed");
		return npc;
	}

	public static Npc spawnNpc(World world, int id, int x, int y, final int time) {

		final Npc npc = new Npc(world, id, x, y);
		world.getServer().getGameEventHandler().submit(() -> {
			npc.setShouldRespawn(false);
			world.registerNpc(npc);
			world.getServer().getGameEventHandler().add(new SingleEvent(world, null, time, "Spawn NPC Timed") {
				public void action() {
					npc.remove();
				}
			});
		}, "Spawn NPC Delayed");
		return npc;
	}

	public static void completeQuest(Player p, QuestInterface quest) {
		p.sendQuestComplete(quest.getQuestId());
	}

	public static int random(int low, int high) {
		return DataConversions.random(low, high);
	}

	/**
	 * Creates a new ground item
	 *
	 * @param id
	 * @param amount
	 * @param x
	 * @param y
	 * @param owner
	 */


	public static void createGroundItem(int id, int amount, int x, int y, Player owner) {
		owner.getWorld().registerItem(new GroundItem(owner.getWorld(), id, x, y, amount, owner));
	}

	/**
	 * Creates a new ground item
	 *
	 * @param id
	 * @param amount
	 * @param x
	 * @param y
	 */
	public static void createGroundItem(World world, int id, int amount, int x, int y) {
		world.registerItem(new GroundItem(world, id, x, y, amount, (Player) null));
	}

	public static void createGroundItemDelayedRemove(final GroundItem i, int time) {
		i.getWorld().getServer().getGameEventHandler().submit(() -> {
			if (i.getLoc() == null) {
				i.getWorld().getServer().getGameEventHandler().add(new SingleEvent(i.getWorld(), null, time, "Spawn Ground Item Timed") {
					public void action() {
						i.getWorld().unregisterItem(i);
					}
				});
			}
		}, "Spawn Ground Item Timed");
	}

	public static void removeNpc(final Npc npc) {
		npc.getWorld().getServer().getGameEventHandler().submit(() -> npc.setUnregistering(true), "Remove NPC");
	}

	/**
	 * Checks if this @param obj id is @param i
	 *
	 * @param obj
	 * @param i
	 * @return
	 */
	public static boolean isObject(GameObject obj, int i) {
		return obj.getID() == i;
	}

	/**
	 * Checks if players quest stage for this quest is @param stage
	 *
	 * @param p
	 * @param qID
	 * @param stage
	 * @return
	 */
	public static boolean atQuestStage(Player p, int qID, int stage) {
		return getQuestStage(p, qID) == stage;
	}

	/**
	 * Checks if players quest stage for this quest is @param stage
	 */
	public static boolean atQuestStage(Player p, QuestInterface quest, int stage) {
		return getQuestStage(p, quest) == stage;
	}

	/**
	 * Returns the quest stage for @param quest
	 *
	 * @param p
	 * @param quest
	 * @return
	 */
	public static int getQuestStage(Player p, QuestInterface quest) {
		return p.getQuestStage(quest);
	}

	/**
	 * Returns the quest stage for @param qID
	 */
	public static int getQuestStage(Player p, int questID) {
		return p.getQuestStage(questID);
	}

	/**
	 * Sets Quest with ID @param questID's stage to @parma stage
	 *
	 * @param p
	 * @param questID
	 * @param stage
	 */
	public static void setQuestStage(final Player p, final int questID, final int stage) {
		p.getWorld().getServer().getGameEventHandler().submit(() -> p.updateQuestStage(questID, stage), "Set Quest Stage");
	}

	/**
	 * Sets @param quest 's stage to @param stage
	 *
	 * @param p
	 * @param quest
	 * @param stage
	 */
	public static void setQuestStage(Player p, QuestInterface quest, int stage) {
		p.updateQuestStage(quest, stage);
	}

	public static void openChest(GameObject obj, int delay, int chestID) {
		GameObject chest = new GameObject(obj.getWorld(), obj.getLocation(), chestID, obj.getDirection(), obj.getType());
		replaceObject(obj, chest);
		delayedSpawnObject(obj.getWorld(), obj.getLoc(), delay);

	}

	public static void replaceObjectDelayed(GameObject obj, int delay, int replaceID) {
		GameObject replaceObj = new GameObject(obj.getWorld(), obj.getLocation(), replaceID, obj.getDirection(), obj.getType());
		replaceObject(obj, replaceObj);
		delayedSpawnObject(obj.getWorld(), obj.getLoc(), delay);
	}

	public static void openChest(GameObject obj, int delay) {
		openChest(obj, delay, 339);
	}

	public static void openChest(GameObject obj) {
		openChest(obj, 2000);
	}

	public static void closeCupboard(GameObject obj, Player p, int cupboardID) {
		replaceObject(obj, new GameObject(obj.getWorld(), obj.getLocation(), cupboardID, obj.getDirection(), obj.getType()));
		p.message("You close the cupboard");
	}

	public static void openCupboard(GameObject obj, Player p, int cupboardID) {
		replaceObject(obj, new GameObject(obj.getWorld(), obj.getLocation(), cupboardID, obj.getDirection(), obj.getType()));
		p.message("You open the cupboard");
	}

	public static void closeGenericObject(GameObject obj, Player p, int objectID, String... messages) {
		replaceObject(obj, new GameObject(obj.getWorld(), obj.getLocation(), objectID, obj.getDirection(), obj.getType()));
		for (String message : messages) {
			p.message(message);
		}
	}

	public static void openGenericObject(GameObject obj, Player p, int objectID, String... messages) {
		replaceObject(obj, new GameObject(obj.getWorld(), obj.getLocation(), objectID, obj.getDirection(), obj.getType()));
		for (String message : messages) {
			p.message(message);
		}
	}

	public static int[] coordModifier(Player player, boolean up, GameObject object) {
		if (object.getGameObjectDef().getHeight() <= 1) {
			return new int[]{player.getX(), Formulae.getNewY(player.getY(), up)};
		}
		int[] coords = {object.getX(), Formulae.getNewY(object.getY(), up)};
		switch (object.getDirection()) {
			case 0:
				coords[1] -= (up ? -object.getGameObjectDef().getHeight() : 1);
				break;
			case 2:
				coords[0] -= (up ? -object.getGameObjectDef().getHeight() : 1);
				break;
			case 4:
				coords[1] += (up ? -1 : object.getGameObjectDef().getHeight());
				break;
			case 6:
				coords[0] += (up ? -1 : object.getGameObjectDef().getHeight());
				break;
		}
		return coords;
	}

	/**
	 * Adds an item to players inventory.
	 */
	public static void addItem(final Player p, final int item, final int amt) {

		p.getWorld().getServer().getGameEventHandler().submit(() -> {
			final Item items = new Item(item, amt);
			if (!items.getDef(p.getWorld()).isStackable() && amt > 1) {
				for (int i = 0; i < amt; i++) {
					p.getInventory().add(new Item(item, 1));
				}
			} else {
				p.getInventory().add(items);
			}
		}, "Add Item");
	}

	/**
	 * Opens a door object for the player and walks through it. Works for any
	 * regular door in any direction.
	 */
	public static void doDoor(final GameObject object, final Player p) {
		doDoor(object, p, 11);
	}

	public static void doTentDoor(final GameObject object, final Player p) {
		p.setBusyTimer(650);
		if (object.getDirection() == 0) {
			if (object.getLocation().equals(p.getLocation())) {
				movePlayer(p, object.getX(), object.getY() - 1);
			} else {
				movePlayer(p, object.getX(), object.getY());
			}
		}
		if (object.getDirection() == 1) {
			if (object.getLocation().equals(p.getLocation())) {
				movePlayer(p, object.getX() - 1, object.getY());
			} else {
				movePlayer(p, object.getX(), object.getY());
			}
		}
		if (object.getDirection() == 2) {
			// DIAGONAL
			// front
			if (object.getX() == p.getX() && object.getY() == p.getY() + 1) {
				movePlayer(p, object.getX(), object.getY() + 1);
			} else if (object.getX() == p.getX() - 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() - 1, object.getY());
			}
			// back
			else if (object.getX() == p.getX() && object.getY() == p.getY() - 1) {
				movePlayer(p, object.getX(), object.getY() - 1);
			} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() + 1, object.getY());
			} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY() + 1) {
				movePlayer(p, object.getX() + 1, object.getY() + 1);
			} else if (object.getX() == p.getX() - 1 && object.getY() == p.getY() - 1) {
				movePlayer(p, object.getX() - 1, object.getY() - 1);
			}
		}
		if (object.getDirection() == 3) {

			// front
			if (object.getX() == p.getX() && object.getY() == p.getY() - 1) {

				movePlayer(p, object.getX(), object.getY() - 1);
			} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() + 1, object.getY());
			}

			// back
			else if (object.getX() == p.getX() && object.getY() == p.getY() + 1) {
				movePlayer(p, object.getX(), object.getY() + 1);
			} else if (object.getX() == p.getX() - 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() - 1, object.getY());
			}

		}
	}

	public static void doWallMovePlayer(final GameObject object, final Player p, int replaceID, int delay, boolean removeObject) {
		p.setBusyTimer(650);
		/* For the odd looking walls. */
		if (removeObject) {
			GameObject newObject = new GameObject(object.getWorld(), object.getLocation(), replaceID, object.getDirection(), object.getType());
			if (object.getID() == replaceID) {
				p.message("Nothing interesting happens");
				return;
			}
			if (replaceID == -1) {
				removeObject(object);
			} else {
				replaceObject(object, newObject);
			}
			delayedSpawnObject(object.getWorld(), object.getLoc(), delay);
		}
		if (object.getDirection() == 0) {
			if (object.getLocation().equals(p.getLocation())) {
				movePlayer(p, object.getX(), object.getY() - 1);
			} else {
				movePlayer(p, object.getX(), object.getY());
			}
		}
		if (object.getDirection() == 1) {
			if (object.getLocation().equals(p.getLocation())) {
				movePlayer(p, object.getX() - 1, object.getY());
			} else {
				movePlayer(p, object.getX(), object.getY());
			}
		}
		if (object.getDirection() == 2) {
			// DIAGONAL
			// front
			if (object.getX() == p.getX() && object.getY() == p.getY() + 1) {
				movePlayer(p, object.getX(), object.getY() + 1);
			} else if (object.getX() == p.getX() - 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() - 1, object.getY());
			}
			// back
			else if (object.getX() == p.getX() && object.getY() == p.getY() - 1) {
				movePlayer(p, object.getX(), object.getY() - 1);
			} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() + 1, object.getY());
			} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY() + 1) {
				movePlayer(p, object.getX() + 1, object.getY() + 1);
			} else if (object.getX() == p.getX() - 1 && object.getY() == p.getY() - 1) {
				movePlayer(p, object.getX() - 1, object.getY() - 1);
			}
		}
		if (object.getDirection() == 3) {

			// front
			if (object.getX() == p.getX() && object.getY() == p.getY() - 1) {
				movePlayer(p, object.getX(), object.getY() - 1);
			} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() + 1, object.getY());
			}

			// back
			else if (object.getX() == p.getX() && object.getY() == p.getY() + 1) {
				movePlayer(p, object.getX(), object.getY() + 1);
			} else if (object.getX() == p.getX() - 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() - 1, object.getY());
			} else if (object.getX() == p.getX() - 1 && object.getY() == p.getY() + 1) {
				movePlayer(p, object.getX() - 1, object.getY() + 1);
			} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY() - 1) {
				movePlayer(p, object.getX() + 1, object.getY() - 1);
			}
		}
	}

	public static void doDoor(final GameObject object, final Player p, int replaceID) {

		p.setBusyTimer(650);
		/* For the odd looking walls. */
		GameObject newObject = new GameObject(object.getWorld(), object.getLocation(), replaceID, object.getDirection(), object.getType());
		if (object.getID() == replaceID) {
			p.message("Nothing interesting happens");
			return;
		}
		if (replaceID == -1) {
			removeObject(object);
		} else {
			p.playSound("opendoor");
			replaceObject(object, newObject);
		}
		delayedSpawnObject(object.getWorld(), object.getLoc(), 3000);

		if (object.getDirection() == 0) {
			if (object.getLocation().equals(p.getLocation())) {
				movePlayer(p, object.getX(), object.getY() - 1);
			} else {
				movePlayer(p, object.getX(), object.getY());
			}
		}
		if (object.getDirection() == 1) {
			if (object.getLocation().equals(p.getLocation())) {
				movePlayer(p, object.getX() - 1, object.getY());
			} else {
				movePlayer(p, object.getX(), object.getY());
			}
		}
		if (object.getDirection() == 2) {
			// front
			if (object.getX() == p.getX() && object.getY() == p.getY() + 1) {

				movePlayer(p, object.getX(), object.getY() + 1);
			} else if (object.getX() == p.getX() - 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() - 1, object.getY());
			}

			// back
			else if (object.getX() == p.getX() && object.getY() == p.getY() - 1) {
				movePlayer(p, object.getX(), object.getY() - 1);
			} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() + 1, object.getY());
			}
		}
		if (object.getDirection() == 3) {

			// front
			if (object.getX() == p.getX() && object.getY() == p.getY() - 1) {

				movePlayer(p, object.getX(), object.getY() - 1);
			} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() + 1, object.getY());
			}

			// back
			else if (object.getX() == p.getX() && object.getY() == p.getY() + 1) {
				movePlayer(p, object.getX(), object.getY() + 1);
			} else if (object.getX() == p.getX() - 1 && object.getY() == p.getY()) {
				movePlayer(p, object.getX() - 1, object.getY());
			}

		}

		// if(dir == 2) {
		// // front
		// if(object.getX() == getLocation().getX() - 1 && object.getY() ==
		// getLocation().getY() ||
		// object.getX() == getLocation().getX() && object.getY() ==
		// getLocation().getY() - 1) {
		// return true;
		// }
		// //back
		// else if(object.getX() == getLocation().getX() + 1 && object.getY() ==
		// getLocation().getY() ||
		// object.getX() == getLocation().getX() && object.getY() ==
		// getLocation().getY() + 1) {
		// return true;
		// }
		// }
		// if(dir == 3) {
		// // front
		// if(object.getX() == getLocation().getX() && object.getY() ==
		// getLocation().getY() - 1 ||
		// object.getX() == getLocation().getX() - 1 && object.getY() ==
		// getLocation().getY() ) {
		// return true;
		// }
		// //back
		// else if(object.getX() == getLocation().getX() && object.getY() ==
		// getLocation().getY() + 1 ||
		// object.getX() == getLocation().getX() + 1 && object.getY() ==
		// getLocation().getY() ) {
		// return true;
		// }
		// }
	}

	public static void doLedge(final GameObject object, final Player p, int damage) {
		p.setBusyTimer(650);
		p.message("you climb the ledge");
		boolean failLedge = !Formulae.calcProductionSuccessful(1, p.getSkills().getLevel(Skills.AGILITY), false, 71);
		if (object != null && !failLedge) {
			if (object.getDirection() == 2 || object.getDirection() == 6) {
				if (object.getX() == p.getX() - 1 && object.getY() == p.getY()) { // X
					if (object.getID() == 753) {
						p.message("and drop down to the cave floor");
						movePlayer(p, object.getX() - 2, object.getY());
					} else {
						p.message("and drop down to the cave floor");
						movePlayer(p, object.getX() - 1, object.getY());
					}
				} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY()) { // Y
					if (object.getID() == 753) {
						p.message("and drop down to the cave floor");
						movePlayer(p, object.getX() + 2, object.getY());
					} else {
						p.message("and drop down to the cave floor");
						movePlayer(p, object.getX() + 1, object.getY());
					}
				}
			}
			if (object.getDirection() == 4 || object.getDirection() == 0) {
				if (object.getX() == p.getX() && object.getY() == p.getY() + 1) { // X
					movePlayer(p, object.getX(), object.getY() + 1);
					p.message("and drop down to the cave floor");
				} else if (object.getX() == p.getX() && object.getY() == p.getY() - 1) { // Y
					movePlayer(p, object.getX(), object.getY() - 1);
				}
			}
		} else {
			p.message("but you slip");
			p.damage(damage);
			playerTalk(p, null, "aargh");
		}
	}

	public static void doRock(final GameObject object, final Player p, int damage, boolean eventMessage,
							  int spikeLocation) {
		p.setBusyTimer(650);
		p.message("you climb onto the rock");
		boolean failRock = !Formulae.calcProductionSuccessful(1, p.getSkills().getLevel(Skills.AGILITY), false, 71);
		if (object != null && !failRock) {
			if (object.getDirection() == 1 || object.getDirection() == 2 || object.getDirection() == 4
				|| object.getDirection() == 3) {
				if (object.getX() == p.getX() - 1 && object.getY() == p.getY()) { // X
					movePlayer(p, object.getX() - 1, object.getY());
				} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY()) { // Y
					movePlayer(p, object.getX() + 1, object.getY());
				} else if (object.getX() == p.getX() && object.getY() == p.getY() + 1) { // left
					// side
					if (object.getID() == 749) {
						movePlayer(p, object.getX(), object.getY() + 1);
					} else {
						movePlayer(p, object.getX() + 1, object.getY());
					}
				} else if (object.getX() == p.getX() && object.getY() == p.getY() - 1) { // right
					// side.
					if (object.getID() == 749) {
						movePlayer(p, object.getX(), object.getY() - 1);
					} else {
						movePlayer(p, object.getX() + 1, object.getY());
					}
				}
			}
			if (object.getDirection() == 6) {
				if (object.getX() == p.getX() && object.getY() == p.getY() + 1) { // left
					// side
					movePlayer(p, object.getX(), object.getY() + 1);
				} else if (object.getX() == p.getX() && object.getY() == p.getY() - 1) { // right
					// side.
					movePlayer(p, object.getX(), object.getY() - 1);
				} else if (object.getX() == p.getX() - 1 && object.getY() == p.getY()) {
					movePlayer(p, object.getX() + 1, object.getY() + 1);
				} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY()) {
					movePlayer(p, object.getX(), object.getY() + 1);
				}
			}
			if (object.getDirection() == 0) {
				if (object.getX() == p.getX() - 1 && object.getY() == p.getY()) { // X
					movePlayer(p, object.getX() - 1, object.getY());
				} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY()) { // Y
					movePlayer(p, object.getX() + 1, object.getY());
				} else if (object.getX() == p.getX() && object.getY() == p.getY() + 1) { // left
					// side
					movePlayer(p, object.getX(), object.getY() + 1);
				} else if (object.getX() == p.getX() && object.getY() == p.getY() - 1) { // right
					// side.
					movePlayer(p, object.getX(), object.getY() - 1);
				}
			}
			if (object.getDirection() == 7) {
				if (object.getX() == p.getX() - 1 && object.getY() == p.getY()) { // X
					movePlayer(p, object.getX() - 1, object.getY() - 1);
				} else if (object.getX() == p.getX() + 1 && object.getY() == p.getY()) { // Y
					movePlayer(p, object.getX() + 1, object.getY());
				} else if (object.getX() == p.getX() && object.getY() == p.getY() + 1) { // left
					// side
					movePlayer(p, object.getX(), object.getY() + 1);
				} else if (object.getX() == p.getX() && object.getY() == p.getY() - 1) { // right
					// side.
					movePlayer(p, object.getX() + 1, object.getY());
				}
			}
			p.message("and step down the other side");
		} else {
			p.message("but you slip");
			p.damage(damage);
			if (spikeLocation == 1) {
				p.teleport(743, 3475);
			} else if (spikeLocation == 2) {
				p.teleport(748, 3482);
			} else if (spikeLocation == 3) {
				p.teleport(738, 3483);
			} else if (spikeLocation == 4) {
				p.teleport(736, 3475);
			} else if (spikeLocation == 5) {
				p.teleport(730, 3478);
			}
			playerTalk(p, null, "aargh");
		}
		if (eventMessage) {
			p.getWorld().getServer().getGameEventHandler()
				.add(new UndergroundPassMessages(p.getWorld(), p, DataConversions.random(2000, 10000)));
		}
	}

	public static void doGate(final Player p, final GameObject object) {
		doGate(p, object, 181);
	}

	public static void removeObject(final GameObject o) {
		o.getWorld().getServer().getGameEventHandler().submit(() -> o.getWorld().unregisterGameObject(o), "Remove Game Object");
	}

	public static void registerObject(final GameObject o) {
		o.getWorld().getServer().getGameEventHandler().submit(() -> o.getWorld().registerGameObject(o), "Add Game Object");
	}

	public static void replaceObject(final GameObject o, final GameObject newObject) {
		o.getWorld().getServer().getGameEventHandler().submit(() -> o.getWorld().replaceGameObject(o, newObject), "Replace Game Object");
	}

	public static void delayedSpawnObject(final World world, final GameObjectLoc loc, final int time) {
		world.getServer().getGameEventHandler().submit(() -> world.delayedSpawnObject(loc, time), "Delayed Add Game Object");
	}

	public static void doGate(final Player p, final GameObject object, int replaceID) {
		doGate(p, object, replaceID, null);
	}

	public static void doGate(final Player p, final GameObject object, int replaceID, Point destination) {
		p.setBusyTimer(650);
		// 0 - East
		// 1 - Diagonal S- NE
		// 2 - South
		// 3 - Diagonal S-NW
		// 4- West
		// 5 - Diagonal N-NE
		// 6 - North
		// 7 - Diagonal N-W
		// 8 - N->S
		p.playSound("opendoor");
		removeObject(object);
		registerObject(new GameObject(object.getWorld(), object.getLocation(), replaceID, object.getDirection(), object.getType()));

		int dir = object.getDirection();
		if (destination != null && Math.abs(p.getX() - destination.getX()) <= 5 && Math.abs(p.getY() - destination.getY()) <= 5) {
			movePlayer(p, destination.getX(), destination.getY());
		} else if (dir == 0) {
			if (p.getX() >= object.getX()) {
				movePlayer(p, object.getX() - 1, object.getY());
			} else {
				movePlayer(p, object.getX(), object.getY());
			}
		} else if (dir == 2) {
			if (p.getY() <= object.getY()) {
				movePlayer(p, object.getX(), object.getY() + 1);
			} else {
				movePlayer(p, object.getX(), object.getY());
			}
		} else if (dir == 4) {
			if (p.getX() > object.getX()) {
				movePlayer(p, object.getX(), object.getY());
			} else {
				movePlayer(p, object.getX() + 1, object.getY());
			}
		} else if (dir == 6) {
			if (p.getY() >= object.getY()) {
				movePlayer(p, object.getX(), object.getY() - 1);
			} else {
				movePlayer(p, object.getX(), object.getY());
			}
		} else {
			p.message("Failure - Contact an administrator");
		}
		sleep(1000);
		registerObject(new GameObject(object.getWorld(), object.getLoc()));
	}

	/**
	 * Gets closest npc within players area.
	 *
	 * @param npcId
	 * @param radius
	 * @return
	 */
	public static Npc getNearestNpc(Player p, final int npcId, final int radius) {
		final Iterable<Npc> npcsInView = p.getViewArea().getNpcsInView();
		Npc closestNpc = null;
		for (int next = 0; next < radius; next++) {
			for (final Npc n : npcsInView) {
				if (n.getID() == npcId) {

				}
				if (n.getID() == npcId && n.withinRange(p.getLocation(), next) && !n.isBusy()) {
					closestNpc = n;
				}
			}
		}
		return closestNpc;
	}

	public static Npc getMultipleNpcsInArea(Player p, final int radius, final int... npcId) {
		final Iterable<Npc> npcsInView = p.getViewArea().getNpcsInView();
		Npc closestNpc = null;
		for (int next = 0; next < radius; next++) {
			for (final Npc n : npcsInView) {
				for (final int na : npcId) {
					if (n.getID() == na && n.withinRange(p.getLocation(), next) && !n.isBusy()) {
						closestNpc = n;
					}
				}
			}
		}
		return closestNpc;
	}

	public static Collection<Npc> getNpcsInArea(Player p, final int radius, final int... npcId) {
		final Iterable<Npc> npcsInView = p.getViewArea().getNpcsInView();
		final Collection<Npc> npcsList = new ArrayList<Npc>();
		for (int next = 0; next < radius; next++) {
			for (final Npc n : npcsInView) {
				for (final int na : npcId) {
					if (n.getID() == na && n.withinRange(p.getLocation(), next) && !n.isBusy()) {
						npcsList.add(n);
					}
				}
			}
		}
		return npcsList;
	}

	public static boolean isNpcNearby(Player p, int id) {
		for (Npc npc : p.getViewArea().getNpcsInView()) {
			if (npc.getID() == id) {
				return true;
			}
		}
		return false;
	}

	//Checks if the player is restricted to wielding a certain item
	public static boolean canWield(Player player, Item item) {
		int requiredLevel = item.getDef(player.getWorld()).getRequiredLevel();
		int requiredSkillIndex = item.getDef(player.getWorld()).getRequiredSkillIndex();
		String itemLower = item.getDef(player.getWorld()).getName().toLowerCase();
		Optional<Integer> optionalLevel = Optional.empty();
		Optional<Integer> optionalSkillIndex = Optional.empty();
		boolean ableToWield = true;
		boolean bypass = !player.getWorld().getServer().getConfig().STRICT_CHECK_ALL &&
			(itemLower.startsWith("poisoned") &&
				((itemLower.endsWith("throwing dart") && !player.getWorld().getServer().getConfig().STRICT_PDART_CHECK) ||
					(itemLower.endsWith("throwing knife") && !player.getWorld().getServer().getConfig().STRICT_PKNIFE_CHECK) ||
					(itemLower.endsWith("spear") && !player.getWorld().getServer().getConfig().STRICT_PSPEAR_CHECK))
			);

		if (itemLower.endsWith("spear") || itemLower.endsWith("throwing knife")) {
			optionalLevel = Optional.of(requiredLevel <= 10 ? requiredLevel : requiredLevel + 5);
			optionalSkillIndex = Optional.of(com.openrsc.server.constants.Skills.ATTACK);
		}
		//staff of iban (usable)
		if (item.getID() == ItemId.STAFF_OF_IBAN.id()) {
			optionalLevel = Optional.of(requiredLevel);
			optionalSkillIndex = Optional.of(com.openrsc.server.constants.Skills.ATTACK);
		}
		//battlestaves (incl. enchanted version)
		if (itemLower.contains("battlestaff")) {
			optionalLevel = Optional.of(requiredLevel);
			optionalSkillIndex = Optional.of(com.openrsc.server.constants.Skills.ATTACK);
		}

		if (player.getSkills().getMaxStat(requiredSkillIndex) < requiredLevel) {
			if (!bypass) {
				player.message("You are not a high enough level to use this item");
				player.message("You need to have a " + player.getWorld().getServer().getConstants().getSkills().getSkillName(requiredSkillIndex) + " level of " + requiredLevel);
				ableToWield = false;
			}
		}
		if (optionalSkillIndex.isPresent() && player.getSkills().getMaxStat(optionalSkillIndex.get()) < optionalLevel.get()) {
			if (!bypass) {
				player.message("You are not a high enough level to use this item");
				player.message("You need to have a " + player.getWorld().getServer().getConstants().getSkills().getSkillName(optionalSkillIndex.get()) + " level of " + optionalLevel.get());
				ableToWield = false;
			}
		}
		if (item.getDef(player.getWorld()).isFemaleOnly() && player.isMale()) {
			player.message("It doesn't fit!");
			player.message("Perhaps I should get someone to adjust it for me");
			ableToWield = false;
		}
		if ((item.getID() == ItemId.RUNE_PLATE_MAIL_BODY.id() || item.getID() == ItemId.RUNE_PLATE_MAIL_TOP.id())
			&& (player.getQuestStage(Quests.DRAGON_SLAYER) != -1)) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to complete the dragon slayer quest");
			return false;
		} else if (item.getID() == ItemId.DRAGON_SWORD.id() && player.getQuestStage(Quests.LOST_CITY) != -1) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to complete the Lost city of zanaris quest");
			return false;
		} else if (item.getID() == ItemId.DRAGON_AXE.id() && player.getQuestStage(Quests.HEROS_QUEST) != -1) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to complete the Hero's guild entry quest");
			return false;
		} else if (item.getID() == ItemId.DRAGON_SQUARE_SHIELD.id() && player.getQuestStage(Quests.LEGENDS_QUEST) != -1) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to complete the legend's guild quest");
			return false;
		}
		/*
		 * Hacky but works for god staffs and god capes.
		 */
		else if (item.getID() == ItemId.STAFF_OF_GUTHIX.id() && (player.getInventory().wielding(ItemId.ZAMORAK_CAPE.id()) || player.getInventory().wielding(ItemId.SARADOMIN_CAPE.id()))) { // try to wear guthix staff
			player.message("you may not wield this staff while wearing a cape of another god");
			return false;
		} else if (item.getID() == ItemId.STAFF_OF_SARADOMIN.id() && (player.getInventory().wielding(ItemId.ZAMORAK_CAPE.id()) || player.getInventory().wielding(ItemId.GUTHIX_CAPE.id()))) { // try to wear sara staff
			player.message("you may not wield this staff while wearing a cape of another god");
			return false;
		} else if (item.getID() == ItemId.STAFF_OF_ZAMORAK.id() && (player.getInventory().wielding(ItemId.SARADOMIN_CAPE.id()) || player.getInventory().wielding(ItemId.GUTHIX_CAPE.id()))) { // try to wear zamorak staff
			player.message("you may not wield this staff while wearing a cape of another god");
			return false;
		} else if (item.getID() == ItemId.GUTHIX_CAPE.id() && (player.getInventory().wielding(ItemId.STAFF_OF_ZAMORAK.id()) || player.getInventory().wielding(ItemId.STAFF_OF_SARADOMIN.id()))) { // try to wear guthix cape
			player.message("you may not wear this cape while wielding staffs of the other gods");
			return false;
		} else if (item.getID() == ItemId.SARADOMIN_CAPE.id() && (player.getInventory().wielding(ItemId.STAFF_OF_ZAMORAK.id()) || player.getInventory().wielding(ItemId.STAFF_OF_GUTHIX.id()))) { // try to wear sara cape
			player.message("you may not wear this cape while wielding staffs of the other gods");
			return false;
		} else if (item.getID() == ItemId.ZAMORAK_CAPE.id() && (player.getInventory().wielding(ItemId.STAFF_OF_GUTHIX.id()) || player.getInventory().wielding(ItemId.STAFF_OF_SARADOMIN.id()))) { // try to wear zamorak cape
			player.message("you may not wear this cape while wielding staffs of the other gods");
			return false;
		}
		/** Quest cape 112QP TODO item id **/
		/*
		else if (item.getID() == 2145 && player.getQuestPoints() < 112) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to complete all the available quests");
			return;
		}*/
		/** Max skill total cape TODO item id **/
		/*else if (item.getID() == 2146 && player.getSkills().getTotalLevel() < 1782) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to be level 99 in all skills");
			return;
		}*/
		/** iron men armours **/
		else if ((item.getID() == ItemId.IRONMAN_HELM.id() || item.getID() == ItemId.IRONMAN_PLATEBODY.id()
			|| item.getID() == ItemId.IRONMAN_PLATELEGS.id()) && !player.isIronMan(IronmanMode.Ironman.id())) {
			player.message("You need to be an Iron Man to wear this");
			return false;
		} else if ((item.getID() == ItemId.ULTIMATE_IRONMAN_HELM.id() || item.getID() == ItemId.ULTIMATE_IRONMAN_PLATEBODY.id()
			|| item.getID() == ItemId.ULTIMATE_IRONMAN_PLATELEGS.id()) && !player.isIronMan(IronmanMode.Ultimate.id())) {
			player.message("You need to be an Ultimate Iron Man to wear this");
			return false;
		} else if ((item.getID() == ItemId.HARDCORE_IRONMAN_HELM.id() || item.getID() == ItemId.HARDCORE_IRONMAN_PLATEBODY.id()
			|| item.getID() == ItemId.HARDCORE_IRONMAN_PLATELEGS.id()) && !player.isIronMan(IronmanMode.Hardcore.id())) {
			player.message("You need to be a Hardcore Iron Man to wear this");
			return false;
		} else if (item.getID() == 2254 && player.getQuestStage(Quests.LEGENDS_QUEST) != -1) {
			player.message("you have not earned the right to wear this yet");
			player.message("you need to complete the Legends Quest");
			return false;
		}
		if (!ableToWield)
			return false;

		return true;
	}

	/**
	 * Checks if player has an item, and returns true/false.
	 *
	 * @param p
	 * @param item
	 * @return
	 */
	public static boolean hasItem(final Player p, final int item) {
		boolean retval = p.getInventory().hasItemId(item);

		return retval;
	}

	/**
	 * Checks if player has item and returns true/false
	 *
	 * @param p
	 * @param id
	 * @param amt
	 * @return
	 */
	public static boolean hasItem(final Player p, final int id, final int amt) {
		int amount = p.getInventory().countId(id);
		int equipslot = -1;
		if (p.getWorld().getServer().getConfig().WANT_EQUIPMENT_TAB) {
			if ((equipslot = p.getEquipment().hasEquipped(id)) != -1) {
				amount += p.getEquipment().get(equipslot).getAmount();
			}
		}
		return amount >= amt;
	}

	/**
	 * Checks if player has an item in bank, and returns true/false.
	 *
	 * @param p
	 * @param item
	 * @return
	 */
	public static boolean hasItemInBank(final Player p, final int item) {
		return p.getBank().hasItemId(item);
	}

	/**
	 * Checks if player has an item including bank, and returns true/false.
	 *
	 * @param p
	 * @param item
	 * @return
	 */
	public static boolean hasItemInclBank(final Player p, final int item) {
		return hasItem(p, item) || hasItemInBank(p, item);
	}

	/**
	 * Displays server message(s) with 2.2 second delay.
	 *
	 * @param player
	 * @param messages
	 */
	public static void message(final Player player, int delay, final String... messages) {
		message(player, null, delay, messages);
	}

	public static void message(final Player player, final Npc npc, int delay, final String... messages) {
		for (final String message : messages) {
			if (!message.equalsIgnoreCase("null")) {
				if (npc != null) {
					if (npc.isRemoved()) {
						player.setBusy(false);
						return;
					}
					npc.setBusyTimer(delay);
				}
				player.setBusy(true);
				player.getWorld().getServer().getGameEventHandler().submit(() -> player.message(message), "Message Player");
			}
			sleep(delay);
		}
		player.setBusy(false);
	}

	/**
	 * Displays server message(s) with 2.2 second delay.
	 *
	 * @param player
	 * @param messages
	 */
	public static void message(final Player player, final String... messages) {
		for (final String message : messages) {
			if (!message.equalsIgnoreCase("null")) {
				if (player.getInteractingNpc() != null) {
					player.getInteractingNpc().setBusyTimer(1900);
				}
				player.getWorld().getServer().getGameEventHandler().submit(() -> player.message("@que@" + message), "Multi Message Player");
				player.setBusyTimer(1900);
			}
			sleep(1900);
		}
		player.setBusyTimer(0);
	}

	/**
	 * Npc chat method
	 *
	 * @param player
	 * @param npc
	 * @param messages - String array of npc dialogue lines.
	 */
	public static void npcTalk(final Player player, final Npc npc, final int delay, final String... messages) {
		for (final String message : messages) {
			if (!message.equalsIgnoreCase("null")) {
				if (npc.isRemoved()) {
					player.setBusy(false);
					return;
				}
				npc.setBusy(true);
				player.setBusy(true);
				player.getWorld().getServer().getGameEventHandler().submit(() -> {
					npc.resetPath();
					player.resetPath();

					npc.getUpdateFlags().setChatMessage(new ChatMessage(npc, message, player));

					npc.face(player);
					if (!player.inCombat()) {
						player.face(npc);
					}
				}, "NPC Talk");
			}

			sleep(delay);

		}
		npc.setBusy(false);
		player.setBusy(false);
	}

	public static void npcTalk(final Player player, final Npc npc, final String... messages) {
		npcTalk(player, npc, 1900, messages);
	}

	/**
	 * Npc chat method not blocking
	 *
	 * @param player
	 * @param npc
	 * @param messages - String array of npc dialogue lines.
	 */
	public static void npcYell(final Player player, final Npc npc, final String... messages) {
		for (final String message : messages) {
			if (!message.equalsIgnoreCase("null")) {
				player.getWorld().getServer().getGameEventHandler().submit(() -> npc.getUpdateFlags().setChatMessage(new ChatMessage(npc, message, player)), "NPC Yell");
			}
		}
	}

	/**
	 * Player message(s), each message has 2.2s delay between.
	 *
	 * @param player
	 * @param npc
	 * @param messages
	 */
	public static void playerTalk(final Player player, final Npc npc, final String... messages) {
		for (final String message : messages) {
			if (!message.equalsIgnoreCase("null")) {
				if (npc != null) {
					if (npc.isRemoved()) {
						player.setBusy(false);
						return;
					}
				}
				player.getWorld().getServer().getGameEventHandler().submit(() -> {
					if (npc != null) {
						npc.resetPath();
						npc.setBusyTimer(2500);
					}
					if (!player.inCombat()) {
						if (npc != null) {
							npc.face(player);
							player.face(npc);
						}
						player.setBusyTimer(2500);
						player.resetPath();
					}
					player.getUpdateFlags().setChatMessage(new ChatMessage(player, message, (npc == null ? player : npc)));
				}, "Talk as Player");
			}
			sleep(1900);
		}
	}

	public static void playerTalk(final Player player, final String message) {
		player.getUpdateFlags().setChatMessage(new ChatMessage(player, message, player));
	}

	/**
	 * Removes an item from players inventory.
	 *
	 * @param p
	 * @param id
	 * @param amt
	 */
	public static boolean removeItem(final Player p, final int id, final int amt) {

		if (!hasItem(p, id, amt)) {
			return false;
		}
		p.getWorld().getServer().getGameEventHandler().submit(() -> {
			final Item item = new Item(id, 1);
			if (!item.getDef(p.getWorld()).isStackable()) {
				p.getInventory().remove(new Item(id, 1));
			} else {
				p.getInventory().remove(new Item(id, amt));
			}
		}, "Remove Ground Item");
		return true;
	}

	/**
	 * Removes an item from players inventory.
	 *
	 * @param p
	 * @param items
	 * @return
	 */
	public static boolean removeItem(final Player p, final Item... items) {
		for (Item i : items) {
			if (!p.getInventory().contains(i)) {
				return false;
			}
		}
		p.getWorld().getServer().getGameEventHandler().submit(() -> {
			for (Item ir : items) {
				p.getInventory().remove(ir);
			}
		}, "Remove Multi Ground Item");
		return true;
	}

	/**
	 * Displays item bubble above players head.
	 *
	 * @param player
	 * @param item
	 */
	public static void showBubble(final Player player, final Item item) {
		final Bubble bubble = new Bubble(player, item.getID());
		player.getUpdateFlags().setActionBubble(bubble);
	}

	public static void showBubble2(final Npc npc, final Item item) {
		final BubbleNpc bubble = new BubbleNpc(npc, item.getID());
		npc.getUpdateFlags().setActionBubbleNpc(bubble);
	}

	/**
	 * Displays item bubble above players head.
	 *
	 * @param player
	 * @param item
	 */
	public static void showBubble(final Player player, final GroundItem item) {
		final Bubble bubble = new Bubble(player, item.getID());
		player.getUpdateFlags().setActionBubble(bubble);
	}

	public static void showPlayerMenu(final Player player, final Npc npc, final String... options) {
		player.resetMenuHandler();
		player.setOption(-5);
		player.setMenuHandler(new MenuOptionListener(options));
		ActionSender.sendMenu(player, options);
	}


	public static int showMenu(final Player player, final String... options) {
		return showMenu(player, null, true, options);
	}

	public static int showMenu(final Player player, final Npc npc, final String... options) {
		return showMenu(player, npc, true, options);
	}

	public static int showMenu(final Player player, final Npc npc, final boolean sendToClient, final String... options) {
		final long start = System.currentTimeMillis();
		if (npc != null) {
			if (npc.isRemoved()) {
				player.resetMenuHandler();
				player.setBusy(false);
				return -1;
			}
			npc.setBusy(true);
		}
		player.setMenuHandler(new MenuOptionListener(options));
		ActionSender.sendMenu(player, options);

		synchronized (player.getMenuHandler()) {
			while (!player.checkUnderAttack()) {
				if (player.getOption() != -1) {
					if (npc != null && options[player.getOption()] != null) {
						npc.setBusy(false);
						if (sendToClient)
							playerTalk(player, npc, options[player.getOption()]);
					}
					return player.getOption();
				} else if (System.currentTimeMillis() - start > 90000 || player.getMenuHandler() == null) {
					player.resetMenuHandler();
					if (npc != null) {
						npc.setBusy(false);
						player.setBusyTimer(0);
					}
					return -1;
				}
				sleep(1);
			}
			player.releaseUnderAttack();
			player.notify();
			//player got busy (combat), free npc if any
			if (npc != null) {
				npc.setBusy(false);
			}
			return -1;
		}
	}

	public static void resetGnomeCooking(Player p) {
		String[] caches = {
			"cheese_on_batta", "tomato_on_batta", "tomato_cheese_batta", "leaves_on_batta",
			"complete_dish", "chocolate_on_bowl", "leaves_on_bowl", "chocolate_bomb", "cream_on_bowl",
			"choco_dust_on_bowl", "aqua_toad_legs", "gnomespice_toad_legs", "toadlegs_on_batta",
			"kingworms_on_bowl", "onions_on_bowl", "gnomespice_on_bowl", "wormhole", "gnomespice_on_dough",
			"toadlegs_on_dough", "gnomecrunchie_dough", "gnome_crunchie_cooked", "gnomespice_on_worm",
			"worm_on_batta", "worm_batta", "onion_on_batta", "cabbage_on_batta", "dwell_on_batta",
			"veg_batta_no_cheese", "veg_batta_with_cheese", "chocolate_on_dough", "choco_dust_on_crunchies",
			"potato_on_bowl", "vegball", "toadlegs_on_bowl", "cheese_on_bowl", "dwell_on_bowl", "kingworm_on_dough",
			"leaves_on_dough", "spice_over_crunchies", "batta_cooked_leaves", "diced_orange_on_batta", "lime_on_batta",
			"pine_apple_batta", "spice_over_batta"
		};
		for (String s : caches) {
			if (p.getCache().hasKey(s)) {
				p.getCache().remove(s);
			}
		}
	}

	public static boolean checkAndRemoveBlurberry(Player p, boolean reset) {
		String[] caches = {
			"lemon_in_shaker", "orange_in_shaker", "pineapple_in_shaker", "lemon_slices_to_drink",
			"drunk_dragon_base", "diced_pa_to_drink", "cream_into_drink", "dwell_in_shaker",
			"gin_in_shaker", "vodka_in_shaker", "fruit_blast_base", "lime_in_shaker", "sgg_base",
			"leaves_into_drink", "lime_slices_to_drink", "whisky_in_shaker", "milk_in_shaker",
			"leaves_in_shaker", "choco_bar_in_drink", "chocolate_saturday_base", "heated_choco_saturday",
			"choco_dust_into_drink", "brandy_in_shaker", "diced_orange_in_drink", "blurberry_special_base",
			"diced_lemon_in_drink", "pineapple_punch_base", "diced_lime_in_drink", "wizard_blizzard_base"
		};
		for (String s : caches) {
			if (p.getCache().hasKey(s)) {
				if (reset) {
					p.getCache().remove(s);
					continue;
				}
				return true;
			}
		}
		return false;
	}

	public static void sleep(final int delayMs) {
		final PluginTask pluginTask = PluginTask.getContextPluginTask();
		if (pluginTask == null)
			return;
		// System.out.println("Sleeping on " + Thread.currentThread().getName());
		final int ticks = (int)Math.ceil((double)delayMs / (double) pluginTask.getWorld().getServer().getConfig().GAME_TICK);
		pluginTask.pause(ticks);
	}

	/**
	 * Transforms npc into another please note that you will need to unregister
	 * the transformed npc after using this method.
	 *
	 * @param n
	 * @param newID
	 * @return
	 */
	public static Npc transform(final Npc n, final int newID, boolean onlyShift) {
		final Npc newNpc = new Npc(n.getWorld(), newID, n.getX(), n.getY());
		n.getWorld().getServer().getGameEventHandler().submit(() -> {
			newNpc.setShouldRespawn(false);
			n.getWorld().registerNpc(newNpc);
			if (onlyShift) {
				n.setShouldRespawn(false);
			}
			n.remove();
		}, "Transform NPC to NPC");
		return newNpc;
	}

	public static void temporaryRemoveNpc(final Npc n) {
		n.getWorld().getServer().getGameEventHandler().submit(() -> {
			n.setShouldRespawn(true);
			n.remove();
		}, "Temporary Remove NPC");
	}


}
