import java.util.Hashtable;


public class boundmodel {
	Obj model ; // the raw model
	Joint bindingpose ; // the binding pose weights are calculated on
	Joint vertexbind[] ; //which joint each vertex is bound to
	
	public boundmodel(Obj model, Joint bindingpose){
		this.model = model ;
		this.bindingpose = bindingpose ;
		rigidbind() ;
	}
	
	//assigns each vertex in the model a single Joint to be bound to
	public void rigidbind(){
		bindingpose.setGlobalTransform() ;
		vertexbind = new Joint[model.vertices.length] ;
		for(int k=0;k<vertexbind.length;k++){
			vertexbind[k] = bindingpose.getClosestJoint(model.vertices[k]) ;
		}
	}
	
	//returns a new model that is this bound model in the given pose
	public Obj pose(Joint pose){
		pose.setBoneLengths(bindingpose) ;
		pose.setGlobalTransform() ;
		bindingpose.setGlobalTransform() ;
		Obj newmodel = new Obj(model) ;
		Hashtable<Joint, Matrix4d> posetoposemap = new Hashtable<Joint, Matrix4d>() ; // save the joint maps
		for(int k=0;k<model.vertices.length;k++){
			
			
			Joint from = vertexbind[k] ;
			Joint to = pose.getJoint(from.name) ;
			
			Matrix4d map = posetoposemap.get(from) ; // look up joint map
			if(map ==null){//calculate it if it hasn't been calculated yet
				
				
				
				Vector3d axis = new Vector3d() ;
				//axis of rotation for line up is cross product
				axis.cross(from.baseoffset, to.baseoffset) ;
				//it's length is sin(angle) * length1 * length2, so angle is arcsin
			//System.out.println(from.baseoffset.length() +" = " + to.baseoffset.length() );
				double angle = Math.asin(axis.length() / (from.baseoffset.length() * to.baseoffset.length())) ;
				
				Quat4d q = new Quat4d()  ;
				
				axis.normalize() ;
				double lengthsquared = axis.lengthSquared();
				q.setAxisAngle(axis.x, axis.y, axis.z, angle) ;
				map = new Matrix4d();
				map.setIdentity() ;
				if(lengthsquared >0.99 && lengthsquared <1.01 ){
					//System.out.println(axis.toString() +" by " + angle) ;
					map.mul(new Matrix4d(q, new Vector3d(), 1)) ;
				}
				Matrix4d m = new Matrix4d();
				
				m.invert(from.globaltransform) ;//invert the model skeleton transforms to get into local coordinates
				
				map.mul(m) ;
				
				map.mul(to.globaltransform,map) ;
				//map.mul(from.globaltransform) ;
				// line up bone in local coordinates
				//map.mul(to.globaltransform) ;//then apply the transform of the pose we want
				
				posetoposemap.put(from,map) ;
				
			}
			//apply the transformation
			Vector4d v = new Vector4d(newmodel.vertices[k].x, newmodel.vertices[k].y, newmodel.vertices[k].z, 1);
			Vector4d n = new Vector4d(newmodel.normals[k].x, newmodel.normals[k].y, newmodel.normals[k].z, 0) ;
			map.transform(v);
			map.transform(n);
			newmodel.vertices[k].x = v.x ;
			newmodel.vertices[k].y = v.y ;
			newmodel.vertices[k].z = v.z ;
			
			newmodel.normals[k].x = n.x ;
			newmodel.normals[k].y = n.y ;
			newmodel.normals[k].z = n.z ;
			
			
		}
		return newmodel ;
	}
}
