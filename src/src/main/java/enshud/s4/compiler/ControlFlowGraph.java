package enshud.s4.compiler;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ControlFlowGraph
{
	public class BasicBlock
	{
		private String name="";
		private ArrayList<CASL.Code> code;
		private ArrayList<BasicBlock> child=new ArrayList<>();
		private ArrayList<BasicBlock> parent=new ArrayList<>();
		private boolean visited=false;
		private HashMap<CASL.Code, HashMap<CASL.Code, String>> in=new HashMap<>();
		private HashMap<CASL.Code, HashMap<CASL.Code, String>> out=new HashMap<>();
		
		public BasicBlock(String name, ArrayList<CASL.Code> code)
		{
			this.name=name;
			this.code=code;
		}
		
		public String toString()
		{
			String s=name+"\n{\n";
			s+=code.stream().map(CASL.Code::toString).reduce("", (joined, t)->joined+t+"\n");
			s+="}->["+String.join(", ", child.stream().map(b->b.name).collect(Collectors.toList()))+"]\n";
			return s;
		}
		
		public boolean isVisited()
		{
			return visited;
		}
		
		public ArrayList<CASL.Code> getCode()
		{
			visited=true;
			return code;
		}
		
		public ArrayList<BasicBlock> getChild()
		{
			return child;
		}
		
		public ArrayList<BasicBlock> getParent()
		{
			return parent;
		}
		
		public HashMap<CASL.Code, HashMap<CASL.Code, String>> getIn()
		{
			return in;
		}
		
		public void setIn(HashMap<CASL.Code, HashMap<CASL.Code, String>> in)
		{
			this.in=in;
		}
		
		public HashMap<CASL.Code, HashMap<CASL.Code, String>> getOut()
		{
			return out;
		}
		
		public void setOut(HashMap<CASL.Code, HashMap<CASL.Code, String>> out)
		{
			this.out=out;
		}
		
		public HashMap<CASL.Code, String> getOut(final CASL.Code code)
		{
			return out.get(code);
		}
	}
	
	private ArrayList<BasicBlock> graph;
	private int cur=0;
	private ArrayList<AbstractMap.SimpleEntry<String, String>> edge=new ArrayList<>();
	
	public ControlFlowGraph(final CASL casl)
	{
		graph=split(casl);
		connectNodes();
	}
	
	public void setUnvisited()
	{
		graph.stream().forEach(b->b.visited=false);
	}
	
	private ArrayList<BasicBlock> split(final CASL casl)
	{
		ArrayList<BasicBlock> basicBlockList=new ArrayList<>();
		ArrayList<CASL.Code> code=new ArrayList<>();
		String name;
		int nblock=0;
		while(cur<casl.getMain().size())
		{
			code.add(casl.getMain().get(cur));
			name=casl.getMain().get(cur).getLabel().isEmpty() ? String.valueOf(nblock) : casl.getMain().get(cur).getLabel();
			for(; cur<casl.getMain().size()-1; ++cur)
			{
				if(!casl.getMain().get(cur+1).getLabel().isEmpty())
				{
					edge.add(new AbstractMap.SimpleEntry<>(name, casl.getMain().get(cur+1).getLabel()));
					break;
				}
				
				if(casl.getMain().get(cur).getInst().isJump()/* || casl.getMain().get(cur).getInst()==CASL.Inst.CALL*/)
				{
					edge.add(new AbstractMap.SimpleEntry<>(name, casl.getMain().get(cur).getOperand().get(0).getELementName()));
					if(casl.getMain().get(cur).getInst()!=CASL.Inst.JUMP)
					{
						edge.add(new AbstractMap.SimpleEntry<>(name, casl.getMain().get(cur+1).getLabel().isEmpty() ? String.valueOf(nblock+1) : casl.getMain().get(cur+1).getLabel()));
					}
					break;
				}
				
				code.add(casl.getMain().get(cur+1));
			}
			basicBlockList.add(new BasicBlock(name, code));
			code=new ArrayList<>();
			++cur;
			++nblock;
		}
		return basicBlockList;
	}
	
	public BasicBlock getRoot()
	{
		return graph.get(0);
	}
	
	private void connectNodes()
	{
		BasicBlock from=null, next=null;
		for(AbstractMap.SimpleEntry<String, String> p : edge)
		{
			for(BasicBlock node : graph)
			{
				if(node.name.equals(p.getKey()))
				{
					from=node;
				}
				if(node.name.equals(p.getValue()))
				{
					next=node;
				}
				if(from!=null && next!=null)
				{
					from.child.add(next);
					next.parent.add(from);
					break;
				}
			}
			if(from==null || next==null)
			{
				System.out.print("not found :["+p.getKey()+", "+p.getValue()+"]\n");
			}
			from=null;
			next=null;
		}
	}
	
	public ArrayList<CASL.Code> getCode()
	{
		ArrayList<CASL.Code> code=new ArrayList<>();
		for(BasicBlock block : graph)
		{
			if(block.isVisited())
			{
				code.addAll(block.getCode());
			}
		}
		return code;
	}
	
	public String toString()
	{
		return graph.stream().map(BasicBlock::toString).reduce("", (joined, s)->joined+s+"\n");
	}
}
