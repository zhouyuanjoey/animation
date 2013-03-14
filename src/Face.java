import java.util.ArrayList;

public class Face {
	public Vector3d[] vertexIndices;
	
	public Face(String[] l) {
		try {
			ArrayList<Vector3d> vertexIndicesArray = new ArrayList<Vector3d>();
			vertexIndices = new Vector3d[1];
			for (int i = 1; i < l.length; i++) {
				String v[] = l[i].split("/");
				vertexIndicesArray.add(new Vector3d(Double.parseDouble(v[0]), 
				Double.parseDouble(v[1]), Double.parseDouble(v[2])));
			}
			vertexIndices = vertexIndicesArray.toArray(vertexIndices);
		}
		catch(Exception e){
			e.printStackTrace() ;
			System.err.print(e) ;
		}
	}
}