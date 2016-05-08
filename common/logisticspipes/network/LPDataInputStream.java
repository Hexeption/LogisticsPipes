package logisticspipes.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.ResourceNetwork;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.order.ClientSideOrderInfo;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.routing.order.LinkedLogisticsOrderList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LPDataInputStream extends DataInputStream implements LPDataInput {

	public LPDataInputStream(byte[] inputBytes) throws IOException {
		super(new ByteArrayInputStream(inputBytes));
	}

	public LPDataInputStream(ByteBuf inputBytes) throws IOException {
		super(new ByteBufInputStream(inputBytes));
	}

	@Override
	public byte[] readByteArray() throws IOException {
		int length = readInt();
		if (length < 0) {
			return null;
		}
		return readBytes(length);
	}

	@Override
	public ForgeDirection readForgeDirection() throws IOException {
		int dir = readByte();
		if (dir == 10) {
			return null;
		}
		return ForgeDirection.values()[dir];
	}

	@Override
	public ExitRoute readExitRoute(World world) throws IOException {
		IRouter destination = readIRouter(world);
		IRouter root = readIRouter(world);
		ForgeDirection exitOri = readForgeDirection();
		ForgeDirection insertOri = readForgeDirection();
		EnumSet<PipeRoutingConnectionType> connectionDetails = readEnumSet(PipeRoutingConnectionType.class);
		double distanceToDestination = readDouble();
		double destinationDistanceToRoot = readDouble();
		int blockDistance = readInt();
		List<DoubleCoordinates> positions = readArrayList(LPDataInput::readLPPosition);
		ExitRoute e = new ExitRoute(root, destination, exitOri, insertOri, destinationDistanceToRoot, connectionDetails, blockDistance);
		e.distanceToDestination = distanceToDestination;
		e.debug.filterPosition = positions;
		e.debug.toStringNetwork = readUTF();
		e.debug.isNewlyAddedCanidate = readBoolean();
		e.debug.isTraced = readBoolean();
		e.debug.index = readInt();
		return e;
	}

	/**
	 * @return ServerRouter or ClientRouter depending where we are
	 * @throws IOException
	 */
	@Override
	public IRouter readIRouter(World world) throws IOException {
		if (readByte() == 0) {
			return null;
		} else {
			DoubleCoordinates pos = readLPPosition();
			TileEntity tile = pos.getTileEntity(world);
			if (tile instanceof LogisticsTileGenericPipe && ((LogisticsTileGenericPipe) tile).pipe instanceof CoreRoutedPipe) {
				return ((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).getRouter();
			}
			return null;
		}
	}

	@Override
	public DoubleCoordinates readLPPosition() throws IOException {
		return new DoubleCoordinates(readDouble(), readDouble(), readDouble());
	}

	@Override
	public <T extends Enum<T>> EnumSet<T> readEnumSet(Class<T> clazz) throws IOException {
		EnumSet<T> types = EnumSet.noneOf(clazz);
		T[] parts = clazz.getEnumConstants();
		int length;
		length = readByte();
		byte[] set = readBytes(length);
		for (T part : parts) {
			if ((set[part.ordinal() / 8] & (1 << (part.ordinal() % 8))) != 0) {
				types.add(part);
			}
		}
		return types;
	}

	@Override
	public byte[] readBytes(int count) throws IOException {
		byte[] bytes = new byte[count];
		int read = in.read(bytes);
		assert read == count;
		return bytes;
	}

	@Override
	public BitSet readBitSet() throws IOException {
		byte size = readByte();
		byte[] bytes = readBytes(size);
		BitSet bits = new BitSet();
		for (int i = 0; i < bytes.length * 8; i++) {
			if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
				bits.set(i);
			}
		}
		return bits;
	}

	@Override
	public NBTTagCompound readNBTTagCompound() throws IOException {
		short legth = readShort();
		if (legth < 0) {
			return null;
		} else {
			byte[] arr = new byte[legth];
			readFully(arr);
			return CompressedStreamTools.func_152457_a(arr, new NBTSizeTracker(Long.MAX_VALUE));
		}

	}

	@Override
	public boolean[] readBooleanArray() throws IOException {
		int length = readInt();
		if (length < 0) {
			return null;
		}
		boolean[] arr = new boolean[length];
		BitSet set = readBitSet();
		for (int i = 0; i < arr.length; i++) {
			arr[i] = set.get(i);
		}
		return arr;
	}

	@Override
	public int[] readIntArray() throws IOException {
		int length = readInt();
		if (length < 0) {
			return null;
		}
		int[] arr = new int[length];
		for (int i = 0; i < length; i++) {
			arr[i] = readInt();
		}
		return arr;
	}

	@Override
	public ItemStack readItemStack() throws IOException {
		final int itemId = readInt();
		if (itemId == 0) {
			return null;
		}

		int stackSize = readInt();
		int damage = readInt();
		ItemStack stack = new ItemStack(Item.getItemById(itemId), stackSize, damage);
		stack.setTagCompound(readNBTTagCompound());
		return stack;
	}

	@Override
	public ItemIdentifier readItemIdentifier() throws IOException {
		final int itemId = readInt();
		if (itemId == 0) {
			return null;
		}

		int damage = readInt();
		NBTTagCompound tag = readNBTTagCompound();
		return ItemIdentifier.get(Item.getItemById(itemId), damage, tag);
	}

	@Override
	public ItemIdentifierStack readItemIdentifierStack() throws IOException {
		int stacksize = readInt();
		if (stacksize == -1) {
			return null;
		}

		ItemIdentifier item = readItemIdentifier();
		return new ItemIdentifierStack(item, stacksize);
	}

	@Override
	public <T> ArrayList<T> readArrayList(IReadListObject<T> reader) throws IOException {
		int size = readInt();
		if (size == -1) {
			return null;
		}

		ArrayList<T> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			list.add(reader.readObject(this));
		}
		return list;
	}

	@Override
	public <T> LinkedList<T> readLinkedList(IReadListObject<T> reader) throws IOException {
		int size = readInt();
		if (size == -1) {
			return null;
		}

		LinkedList<T> list = new LinkedList<>();
		for (int i = 0; i < size; i++) {
			list.add(reader.readObject(this));
		}
		return list;
	}

	@Override
	public <T> Set<T> readSet(IReadListObject<T> handler) throws IOException {
		int size = readInt();
		Set<T> set = new HashSet<>(size);
		for (int i = 0; i < size; i++) {
			set.add(handler.readObject(this));
		}
		return set;
	}

	@Override
	public IOrderInfoProvider readOrderInfo() throws IOException {
		ItemIdentifierStack stack = readItemIdentifierStack();
		int routerId = readInt();
		boolean isFinished = readBoolean();
		boolean inProgress = readBoolean();
		ResourceType type = readEnum(ResourceType.class);
		List<Float> list = readArrayList(LPDataInput::readFloat);
		byte machineProgress = readByte();
		DoubleCoordinates pos = readLPPosition();
		ItemIdentifier ident = readItemIdentifier();
		return new ClientSideOrderInfo(stack, isFinished, type, inProgress, routerId, list, machineProgress, pos, ident);
	}

	@Override
	public <T extends Enum<T>> T readEnum(Class<T> clazz) throws IOException {
		return clazz.getEnumConstants()[readInt()];
	}

	@Override
	public LinkedLogisticsOrderList readLinkedLogisticsOrderList() throws IOException {
		LinkedLogisticsOrderList list = new LinkedLogisticsOrderList();
		list.addAll(readArrayList(LPDataInput::readOrderInfo));
		list.getSubOrders().addAll(readArrayList(LPDataInput::readLinkedLogisticsOrderList));
		return list;
	}

	@Override
	public ByteBuf readByteBuf() throws IOException {
		byte[] bytes = readByteArray();
		return Unpooled.copiedBuffer(bytes);
	}

	@Override
	public long[] readLongArray() throws IOException {
		int length = readInt();
		if (length == -1) {
			return null;
		}

		long[] arr = new long[length];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = readLong();
		}
		return arr;
	}

	@Override
	public IResource readResource() throws IOException {
		return ResourceNetwork.readResource(this);
	}
}
