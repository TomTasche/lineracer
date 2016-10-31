package linebox;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.DistanceJointDef;

public class Main extends JComponent {

	private static final long serialVersionUID = -5954920354957612678L;

	private static final Vec2 GRAVITY = new Vec2(0.0f, 100.0f);
	private static final int VELOCITY_ITERATIONS = 6;
	private static final int POSITION_ITERATIONS = 2;
	private static final float LINE_LENGTH = 10;
	private static final Vec2 SLIDER_SIZE_HALF = new Vec2(30, 10);
	private static final Vec2 RIDER_SIZE_HALF = new Vec2(5, 10);

	private class Mouse extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			synchronized (lines) {
				lines.add(new LinkedList<Vec2>());
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			Point mouse = e.getPoint();
			Vec2 point = new Vec2(mouse.x, mouse.y);

			synchronized (lines) {
				LinkedList<Vec2> lastLine = lines.getLast();

				if (lastLine.size() > 0) {
					Vec2 last = lastLine.getLast();
					if (last.sub(point).length() < LINE_LENGTH)
						return;

					BodyDef bd = new BodyDef();
					bd.position.set(0, 0);
					bd.type = BodyType.STATIC;

					PolygonShape shape = new PolygonShape();
					shape.set(new Vec2[] {last, point}, 2);

					FixtureDef fd = new FixtureDef();
					fd.shape = shape;

					Body body = world.createBody(bd);
					body.createFixture(fd);
				}

				lastLine.add(point);
			}
		}
	}

	private final World world;
	private final Body slide;
	private final Body rider;
	private long lastNano;

	private LinkedList<LinkedList<Vec2>> lines = new LinkedList<>();

	public Main() {
		world = new World(GRAVITY, true);

		{
			BodyDef bd = new BodyDef();
			bd.position.set(100, 100);
			bd.type = BodyType.DYNAMIC;

			PolygonShape shape = new PolygonShape();
			shape.setAsBox(SLIDER_SIZE_HALF.x, SLIDER_SIZE_HALF.y);

			FixtureDef fd = new FixtureDef();
			fd.shape = shape;
			fd.density = 0.2f;
			fd.friction = 0;
			fd.restitution = 0.1f;

			slide = world.createBody(bd);
			slide.createFixture(fd);
		}

		{
			BodyDef bd = new BodyDef();
			bd.position.set(100, 80);
			bd.type = BodyType.DYNAMIC;

			PolygonShape shape = new PolygonShape();
			shape.setAsBox(RIDER_SIZE_HALF.x, RIDER_SIZE_HALF.y);

			FixtureDef fd = new FixtureDef();
			fd.shape = shape;
			fd.density = 0.1f;
			fd.friction = 1;
			fd.restitution = 0.1f;

			rider = world.createBody(bd);
			rider.createFixture(fd);
		}

		{
			DistanceJointDef jd = new DistanceJointDef();
			jd.frequencyHz = 4.0f;
			jd.dampingRatio = 0.5f;
			jd.bodyA = slide;
			jd.bodyB = rider;
			jd.length = jd.bodyA.getWorldPoint(jd.localAnchorA)
					.sub(jd.bodyB.getWorldPoint(jd.localAnchorB)).length() + 5;
			jd.collideConnected = true;
			world.createJoint(jd);
		}

		Mouse mouse = new Mouse();
		addMouseListener(mouse);
		addMouseMotionListener(mouse);

		lastNano = System.nanoTime();
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		if (!(graphics instanceof Graphics2D))
			throw new IllegalStateException();
		Graphics2D g = (Graphics2D) graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		long nano = System.nanoTime();
		float timeStep = (nano - lastNano) / 1000000000f;
		lastNano = nano;

		world.step(timeStep, VELOCITY_ITERATIONS, POSITION_ITERATIONS);

		synchronized (lines) {
			for (List<Vec2> line : lines) {
				Vec2 p1 = null;
				for (Vec2 p2 : line) {
					if (p1 != null)
						g.drawLine((int) p1.x, (int) p1.y, (int) p2.x,
								(int) p2.y);
					p1 = p2;
				}
			}
		}

		{
			Vec2 position = slide.getWorldCenter();
			float angle = slide.getAngle();

			int x = (int) -SLIDER_SIZE_HALF.x;
			int y = (int) -SLIDER_SIZE_HALF.y;
			int w = (int) (SLIDER_SIZE_HALF.x * 2);
			int h = (int) (SLIDER_SIZE_HALF.y * 2);
			AffineTransform tmp = g.getTransform();
			g.translate(position.x, position.y);
			g.rotate(angle);
			g.drawRect(x, y, w, h);
			g.setTransform(tmp);
		}

		{
			Vec2 position = rider.getWorldCenter();
			float angle = rider.getAngle();

			int x = (int) -RIDER_SIZE_HALF.x;
			int y = (int) -RIDER_SIZE_HALF.y;
			int w = (int) (RIDER_SIZE_HALF.x * 2);
			int h = (int) (RIDER_SIZE_HALF.y * 2);
			AffineTransform tmp = g.getTransform();
			g.translate(position.x, position.y);
			g.rotate(angle);
			g.drawRect(x, y, w, h);
			g.setTransform(tmp);
		}

		repaint();
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.add(new Main());
		frame.setSize(800, 500);
		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

}