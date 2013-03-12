import java.awt.Point;

/**
 * Computes an arcBall animation on a volume : each mouse dragging computes a new rotation.
 * Reference http://www.java-tips.org/other-api-tips/jogl/arcball-rotation-nehe-tutorial-jogl-port.html
 * @author laurent BERNABE
 *
 */
public class ArcBall {
        /**
         * A nearly-zero value
         */
        private static final float EPSILON = 1.0e-5f;
       
        private Vector3d savedClickVector = new Vector3d();
        private Vector3d savedDragVector = new Vector3d();
        private float adjustWidth, adjustHeight;
       
        /**
         * Builds an arcball from a frame size
         * @param frameWidth - float - frame width
         * @param frameHeight - float - frame width
         */
        public ArcBall(float frameWidth, float frameHeight){
                setBounds(frameWidth, frameHeight);
        }
       
        /**
         * Binds a screen point and a point on the sphere : result is givent in vector parameter
         * @param screenPoint - java.awt.Point - screen point (MouseEvent#getPoint())
         * @param vector - Vector3d - the place in which store the result
         */
        public void mapToSphere(double x, double y, Vector3d vector){
                Vector3d tempPoint = new Vector3d(x, y,0);
               
                // tempoint must be in range [-1,1]
                tempPoint.x = (tempPoint.x * adjustWidth) - 1.0f;
                tempPoint.y = 1.0f - (tempPoint.y * adjustHeight);
               
                // square of length of point from the screen center
               double length = tempPoint.x*tempPoint.x + tempPoint.y*tempPoint.y;
               
                // if point is mapped outside the sphere : length > radius squared
                if (length > 1.0) {
                        // compute a normalizing factor : radius / sqrt(length)
                        float norm = (float) (1.0 / Math.sqrt(length));
                       
                        // return the normalized vector : a point on the sphere
                        vector.x = tempPoint.x * norm;
                        vector.y = tempPoint.y * norm;
                        vector.z = 0.0f;
                }
                else {
                        // return a vector to a point mapped inside the sphere :
                        // sqrt(radius squared - length)
                        vector.x = tempPoint.x;
                        vector.y = tempPoint.y;
                        vector.z = (float) Math.sqrt(1.0f - length);
                }
        }

        /**
         * Converts a size to the range [-1,1] for each of its component (width, height)
         * @param newWidth - float - width to convert
         * @param newHeight - float - height to convert
         */
        public void setBounds(float newWidth, float newHeight) {
                assert newWidth > 1.0f;
                assert newHeight > 1.0f;
               
                adjustWidth = 1.0f / ( (newWidth - 1.0f) *0.5f);
                adjustHeight = 1.0f / ( (newHeight - 1.0f) * 0.5f);
        }
       
        /**
         * Reacts to a click
         * @param newPoint - java.awt.Point - the clicking point (MouseEvent#getPoint())
         */
        public void click(double x, double y){
                mapToSphere(x, y, savedClickVector);
        }
       
        /**
         * Reacts to a drag
         * @param newPoint - java.awt.Point - the clicking point (MouseEvent#getPoint())
         * @return Quaternion - the new quaternion storing the rotation
         */
        public Quat4d drag(double x, double y){
                mapToSphere(x,y, savedDragVector);
               
                Vector3d perpendicular = new Vector3d();
                perpendicular.cross( savedClickVector, savedDragVector);
                if ( perpendicular.length() > EPSILON ) { // It is non-zero
                        return new Quat4d(savedClickVector.dot(savedDragVector),
                                        perpendicular.x, perpendicular.y, perpendicular.z);
                }
                else {
                        // As begin and end vector coincide => identity quaternion
                        return new Quat4d(0.0f, 0.0f, 0.0f, 0.0f);
                }
        }
}
