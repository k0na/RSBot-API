package org.powerbot.script.rt4;

import org.powerbot.script.Vector3;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Model {

	private final ClientContext ctx;

	private int[] verticesX;
	private int[] verticesY;
	private int[] verticesZ;
	private int[] indicesX;
	private int[] indicesY;
	private int[] indicesZ;

	private int[] originalVerticesX;
	private int[] originalVerticesZ;
	private int[] originalIndicesX;
	private int[] originalIndicesZ;

	public Model(final ClientContext ctx, final int[] verticesX, final int[] verticesY, final int[] verticesZ,
				 final int[] indicesX, final int[] indicesY, final int[] indicesZ) {
		this.ctx = ctx;
		this.verticesX = verticesX;
		this.verticesY = verticesY;
		this.verticesZ = verticesZ;
		this.indicesX = indicesX;
		this.indicesY = indicesY;
		this.indicesZ = indicesZ;

		this.originalVerticesX = verticesX.clone();
		this.originalVerticesZ = verticesZ.clone();
		this.originalIndicesX = indicesX.clone();
		this.originalIndicesZ = indicesZ.clone();
	}

	public static Model fromCacheModel(final ClientContext ctx, final CacheModelConfig model) {
		return new Model(ctx, model.verticesX.clone(), model.verticesY.clone(), model.verticesZ.clone(),
			model.indicesX.clone(), model.indicesY.clone(), model.indicesZ.clone());
	}

	/**
	 * Gets all the polygons of the model
	 * @param localX the local x of the entity
	 * @param localY the local y of the entity
	 * @param orientation the orientation of the entity
	 * @return a list of polygons
	 */
	public List<Polygon> polygons(final int localX, final int localY, int orientation) {
		orientation = ((orientation & 0x3FFF) + 1024) % 2048;
		setOrientation(8192);
		if (orientation != 0) {
			setOrientation(orientation);
		}

		final int[] indX = indicesX();
		final int[] indY = indicesY();
		final int[] indZ = indicesZ();

		final int[] vertX = verticesX();
		final int[] vertY = verticesY();
		final int[] vertZ = verticesZ();

		final java.util.List<Polygon> polys = new ArrayList<>();
		final boolean resizable = ctx.game.resizable();
		for (int i = 0; i < indX.length; i++) {
			if (i >= indY.length && i >= indZ.length) {
				return null;
			}

			final Point x = ctx.game.worldToScreen(localX - vertX[indX[i]], localY - vertZ[indX[i]], -vertY[indX[i]], resizable);
			final Point y = ctx.game.worldToScreen(localX - vertX[indY[i]], localY - vertZ[indY[i]], -vertY[indY[i]], resizable);
			final Point z = ctx.game.worldToScreen(localX - vertX[indZ[i]], localY - vertZ[indZ[i]], -vertY[indZ[i]], resizable);

			if (ctx.game.inViewport(x, resizable) && ctx.game.inViewport(y, resizable) && ctx.game.inViewport(z, resizable)) {
				polys.add(new Polygon(
					new int[]{x.x, y.x, z.x},
					new int[]{x.y, y.y, z.y},
					3));
			}
		}

		return polys;
	}

	/**
	 * Gets all the polygons of the model
	 * @param localX the local x of the entity
	 * @param localY the local y of the entity
	 * @param orientation the orientation of the entity
	 * @param graphics graphics object to draw with
	 */
	public void draw(final int localX, final int localY, final int orientation, final Graphics graphics) {
		for (final Polygon polygon : polygons(localX, localY, orientation)) {
			graphics.drawPolygon(polygon);
		}
	}

	private void setOrientation(final int orientation) {
		final int sin = Game.ARRAY_SIN[orientation];
		final int cos = Game.ARRAY_COS[orientation];
		for (int i = 0; i < originalVerticesX.length; ++i) {
			verticesX[i] = originalVerticesX[i] * cos + originalVerticesZ[i] * sin >> 16;
			verticesZ[i] = originalVerticesZ[i] * cos - originalVerticesX[i] * sin >> 16;

		}
	}

	public Vector3[][] vectors() {
		final Vector3[][] vectors = new Vector3[indicesX().length][3];

		final int[] indX = indicesX();
		final int[] indY = indicesY();
		final int[] indZ = indicesZ();

		final int[] vertX = verticesX();
		final int[] vertY = verticesY();
		final int[] vertZ = verticesZ();

		for (int i = 0; i < vectors.length; i++) {
			vectors[i][0] = new Vector3(vertX[indX[i]], vertX[indY[i]], vertX[indZ[i]]);
			vectors[i][1] = new Vector3(vertY[indX[i]], vertY[indY[i]], vertY[indZ[i]]);
			vectors[i][2] = new Vector3(vertZ[indX[i]], vertZ[indY[i]], vertZ[indZ[i]]);
		}
		return vectors;
	}

	public List<Point> points(final int localX, final int localY, final int orientation) {
		return polygons(localX, localY, orientation).stream().map(p -> {
			final int x = IntStream.of(p.xpoints).sum() / p.npoints;
			final int y = IntStream.of(p.ypoints).sum() / p.npoints;

			return new Point(x, y);
		}).collect(Collectors.toList());
	}

	public Point centerPoint(final int localX, final int localY, final int orientation) {
		final List<Point> points = points(localX, localY, orientation);
		if (points.size() == 0) {
			return new Point(-1, -1);
		}
		final int xTotal = points.stream().mapToInt(p -> p.x).sum();
		final int yTotal = points.stream().mapToInt(p -> p.y).sum();
		final Point central = new Point(xTotal / points.size(), yTotal / points.size());

		return points.stream().filter(p -> ctx.game.inViewport(p)).sorted((a, b) -> {
			double distA = Math.sqrt(((central.x - a.x) * (central.x - a.x)) + ((central.y - a.y) * (central.y - a.y)));
			double distB = Math.sqrt(((central.x - b.x) * (central.x - b.x)) + ((central.y - b.y) * (central.y - b.y)));

			if (distB > distA) {
				return -1;
			} else if (distB == distA) {
				return 0;
			}
			return 1;
		}).collect(Collectors.toList()).get(0);
	}

	public void mirrorModel() {
		for(int i = 0; i < this.originalVerticesZ.length; ++i) {
			this.verticesZ[i] = -this.originalVerticesZ[i];
		}

		for(int i = 0; i < this.originalIndicesX.length; ++i) {
			final int oldX = this.originalIndicesX[i];
			this.indicesX[i] = this.originalIndicesZ[i];
			this.indicesZ[i] = oldX;
		}
	}

	public void rotate(final int num) {
		for (int n = 0; n < num; n++) {
			for (int i = 0; i < originalVerticesX.length; ++i) {
				final int oldX = originalVerticesX[i];
				originalVerticesX[i] = originalVerticesZ[i];
				originalVerticesZ[i] = -oldX;
			}
		}

		this.verticesX = originalVerticesX;
		this.verticesZ = originalVerticesZ;
	}


	public int[] verticesX() {
		return verticesX;
	}

	public int[] verticesY() {
		return verticesY;
	}

	public int[] verticesZ() {
		return verticesZ;
	}

	public int[] indicesX() {
		return indicesX;
	}

	public int[] indicesY() {
		return indicesY;
	}

	public int[] indicesZ() {
		return indicesZ;
	}
}
