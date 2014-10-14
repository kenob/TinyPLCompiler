import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* 		OO PARSER AND BYTE-CODE GENERATOR FOR TINY PL
 
Grammar for TinyPL (using EBNF notation) is as follows:

 program ->  decls stmts end
 decls   ->  int idlist ;
 idlist  ->  id { , id } 
 stmts   ->  stmt [ stmts ]
 cmpdstmt->  '{' stmts '}'
 stmt    ->  assign | cond | loop
 assign  ->  id = expr ;
 cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
 loop    ->  while '(' rexp ')' cmpdstmt  
 rexp    ->  expr (< | > | =) expr
 expr    ->  term   [ (+ | -) expr ]
 term    ->  factor [ (* | /) term ]
 factor  ->  int_lit | id | '(' expr ')'
 
Lexical:   id is a single character; 
	      int_lit is an unsigned integer;
		 equality operator is =, not ==

Sample Program: Factorial
 
int n, i, f;
n = 4;
i = 1;
f = 1;
while (i < n) {
  i = i + 1;
  f= f * i;
}
end

   Sample Program:  GCD
   
int x, y;
x = 121;
y = 132;
while (x != y) {
  if (x > y) 
       { x = x - y; }
  else { y = y - x; }
}
end

 */

public class Parser {
	static HashMap<Character, Integer> id_map = new HashMap<Character, Integer>();
	static HashMap<Character, Integer> id_value_map = new HashMap<Character, Integer>();
	static int id = 0;
	public static void main(String[] args)  {
		System.out.println("Enter program and terminate with 'end'!\n");
		Lexer.lex();
		Code.setCodes();
		Code.bytesToAdd();
		Program p = new Program();
		Code.output();
	}
}

class Program {
	public Program(){
			 Decls d = new Decls();
			 Stmts s = new Stmts();
			 Code.out.add("return");
	}
}

class Decls {
	public Decls(){
		if(Lexer.nextToken == Token.KEY_INT){
			Lexer.lex();
			Idlist idl = new Idlist();	
		}
	}
}

class Idlist {
	public Idlist(){
		while(Lexer.nextToken != Token.SEMICOLON)
		{
			if (Lexer.nextToken == Token.COMMA)
			{
				Lexer.lex();
				
			} else if (Lexer.nextToken == Token.ID) {
				Parser.id_map.put(Lexer.ident, Parser.id++);
				Lexer.lex();
			} else {
				break;
			}
		}
	}
}

class Stmt {
	Assign a;
	Cond c;
	Loop l;
	public Stmt(){
		if(Lexer.nextToken == Token.KEY_WHILE){
			l = new Loop();
		} else if (Lexer.nextToken == Token.KEY_IF){
			c = new Cond();
		} else if (Lexer.nextToken == Token.ID) {
			Lexer.lex();	// skipping over equality
			a = new Assign();
			
		}
		
	}
	 
} 

class Stmts {
	public Stmts()
	 {	
		 new Stmt();
		 if (Lexer.nextToken != Token.KEY_END && Lexer.nextToken != Token.RIGHT_BRACE)
		 {	
			 Lexer.lex();
			 new Stmts();
		 }
	 
	 }
}

class Assign {
	static int resultOfFactor;
	static int resultOfTerm = 1;
	static int resultOfExpr = 0;
	static Boolean firstTermCovered = false;
	static Boolean firstFactorCovered = false;
	static char assignment;
	
	public Assign(){
		assignment = Lexer.ident;
		Lexer.lex(); // moving over to first part of the expression
		
		new Expr();
		Parser.id_value_map.put(assignment, Assign.resultOfExpr);
		int storeID = Parser.id_map.get(assignment);
		Code.out.add("istore_" + storeID);		
		firstTermCovered = false;
		firstFactorCovered = false;
		resultOfTerm = 1;
		resultOfExpr = 0;
		
	}
	
	
}

class Cond {
	Cmpdstmt st;
	int position;
	public Cond(){
		if(Lexer.nextToken==Token.KEY_IF){
	 		//mark this point as an icmp location. could be replaced by a simple variable, but this works for now
			Code.destinations.push(Code.out.size());
			Lexer.lex();
			Rexpr r = new Rexpr();
			st = new Cmpdstmt();
			Lexer.lex();
			if(Lexer.nextToken==Token.KEY_ELSE){
				position = Code.positions.pop();
				//set this as the icmp destination for the if part, and mark this point as a location
				Code.positions.push(Code.out.size());
				Code.out.add("goto");
				Code.out.add("Skip");
				Code.out.add("Skip");
				Code.out.set(position, Code.out.get(position)+ " " + Integer.toString(Code.out.size()));
				//mark this point as an icmpq destination
				Lexer.lex();
				st = new Cmpdstmt();
			}
			int position = Code.positions.pop();
			Code.out.set(position, Code.out.get(position)+ " " + Integer.toString(Code.out.size()));
		}
		 
	}
}

class Loop {
	public Loop(){
		if(Lexer.nextToken==Token.KEY_WHILE){
			Lexer.lex();
 		//mark this point as an icmp location. could be replaced by a simple variable, but this works for now
			Code.gotoDests.push(Code.out.size());
			Rexpr r = new Rexpr();
		}
		Cmpdstmt st = new Cmpdstmt();
		System.out.println(Lexer.nextChar);
		int goToPoint = Code.gotoDests.pop();
		int position = Code.positions.pop();
		Code.out.add("goto " + goToPoint);
 		Code.out.add("Skip");
 		Code.out.add("Skip");
		Code.out.set(position, Code.out.get(position)+ " " + Integer.toString(Code.out.size()));
	}
	 
}

class Cmpdstmt {
	 public Cmpdstmt(){
		 if(Lexer.nextToken==Token.LEFT_BRACE){
			 Lexer.lex();
			 new Cmpdstmt();
		 }
		 else if(Lexer.nextToken==Token.RIGHT_BRACE){
			 //quietly leave
			 Lexer.lex();
		 }
		 else{
			 new Stmts();
			 new Cmpdstmt();
		 }
	 }
}

class Rexpr {
	Rexpr r;
	static String cmp;
	public Rexpr(){
	 	if(Lexer.nextToken== Token.LEFT_PAREN)
	 	{
	 		Lexer.lex();
	 		r = new Rexpr();
	 	}
	 	else if(Lexer.nextToken== Token.LESSER_OP)
	 	{
	 		cmp = "if_icmpge";	
	 		Lexer.lex();
	 		r = new Rexpr();
	 	}
	 	else if(Lexer.nextToken== Token.GREATER_OP)
	 	{
	 		cmp = "if_icmple";
	 		Lexer.lex();
	 		r = new Rexpr();
	 	}
	 	else if(Lexer.nextToken== Token.NOT_EQ)
	 	{
	 		cmp = "if_icmpne";
	 		Lexer.lex();
	 		r = new Rexpr();
	 	}
	 	//representing equals for now
	 	else if(Lexer.nextToken== Token.ASSIGN_OP)
	 	{
	 		cmp = "if_icmpeq";	
	 		Lexer.lex();
	 		r = new Rexpr();
	 	}
	 	else if(Lexer.nextToken== Token.INT_LIT)
	 	{
	 		Lexer.lex();
	 		//TODO: use the correct push command
	 		Code.out.add("iconst_" + Lexer.intValue);
	 		r = new Rexpr();
	 	}
	 	else if(Lexer.nextToken== Token.ID)
	 	{
	 		Lexer.lex();
	 		Code.out.add("iload_" + Parser.id_map.get(Lexer.ident));
	 		r = new Rexpr();
	 	}
	 	else if(Lexer.nextToken== Token.RIGHT_PAREN)
	 	{
	 		Code.positions.push(Code.out.size());
	 		Code.out.add(cmp);
	 		Code.out.add("Skip");
	 		Code.out.add("Skip");
	 		Lexer.lex();	 		
	 	}
	}
}

class Expr {  
	Term t;
	Expr e;
	char op;

	public Expr() {
		t = new Term();
		
		if (Assign.firstTermCovered == false) {
			Assign.resultOfExpr = Assign.resultOfTerm;
			Assign.firstTermCovered = true;
		}
		
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			op = Lexer.nextChar;
			int termresult = Assign.resultOfTerm;
			Lexer.lex();
			e = new Expr();
			
			switch(op){
			case '+':
				Assign.resultOfExpr = termresult + Assign.resultOfExpr;
				
				break;
			case '-':
				Assign.resultOfExpr = termresult - Assign.resultOfExpr;
				break;
			default:
				break;
			}
			
			Code.out.add(Code.getCode(op));
		} else if (Lexer.nextToken != Token.ADD_OP && Lexer.nextToken != Token.SUB_OP){
			Assign.resultOfExpr = Assign.resultOfTerm;
		}
		
	}
}

class Term {  
	Factor f;
	Term t;
	char op;

	public Term() {
		f = new Factor();
		
		if (Assign.firstFactorCovered == false) {
			Assign.resultOfTerm = Assign.resultOfFactor;
			Assign.firstFactorCovered = true;
		}
		
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.nextChar;
			int factresult = Assign.resultOfFactor;
			Lexer.lex();
			t = new Term();
			
			switch(op){
			case '*':
				Assign.resultOfTerm = factresult * Assign.resultOfTerm;
				break;
			case '/':
				Assign.resultOfTerm = factresult / Assign.resultOfTerm;
				break;
			default:
				break;
			}
			
			Code.out.add(Code.getCode(op));
		} else if (Lexer.nextToken != Token.MULT_OP && Lexer.nextToken != Token.DIV_OP){
			Assign.resultOfTerm = Assign.resultOfFactor;
		}
	}
}

class Factor {  
	Expr e;
	int i;

	public Factor() {
		switch (Lexer.nextToken) {
		case Token.INT_LIT: // number
			Assign.resultOfFactor = Lexer.intValue;
			Lexer.lex();
			if (Lexer.intValue >=0 && Lexer.intValue < 6){
				Code.out.add("iconst_"+ Assign.resultOfFactor);
			} else if (Lexer.intValue >=6 && Lexer.intValue < 128) {
				Code.out.add("bipush "+ Assign.resultOfFactor);
				Code.out.add("Skip");
			} else if (Lexer.intValue >= 128) {
				Code.out.add("sipush "+ Assign.resultOfFactor);
			}
			
			break;
		case Token.ID:
			Lexer.lex();
			Assign.resultOfFactor = Parser.id_value_map.get(Lexer.ident);
			Code.out.add("iload_" + Parser.id_map.get(Lexer.ident));
			break;
		case Token.LEFT_PAREN: // '('
			Lexer.lex();
			e = new Expr();
			Assign.resultOfFactor = Assign.resultOfExpr;
			Lexer.lex(); // skip over ')'
			break;
		default:
			break;
		}
	}	 
}

class Code {
	static HashMap<Character, String> opcode= new HashMap<Character, String>();
	static HashMap<String, Integer> bytesToAddCode = new HashMap<String, Integer>();
	public static void setCodes(){
		opcode.put('*', "imul");
		opcode.put('-', "isub");
		opcode.put('+', "iadd");
		opcode.put('/', "idiv");
	}
	
	public static void bytesToAdd(){
		bytesToAddCode.put("iconst", 1);
		bytesToAddCode.put("istore", 1);
		bytesToAddCode.put("bipush", 2);
		bytesToAddCode.put("sipush", 3);
		bytesToAddCode.put("imul", 1);
		bytesToAddCode.put("isub", 1);
		bytesToAddCode.put("iadd", 1);
		bytesToAddCode.put("idiv", 1);
		bytesToAddCode.put("return", 1);
		bytesToAddCode.put("iload", 1);
		
	}
	
	public static String getCode(char a){
		return opcode.get(a);
	}

	static ArrayList<String> out = new ArrayList<String>();
	static Stack<Integer> positions = new Stack<Integer>();
	static Stack<Integer> destinations = new Stack<Integer>();
	static Stack<Integer> gotoDests = new Stack<Integer>();
	public static void output(){
		int bytecount = 0;
		Pattern p = Pattern.compile("([a-z]+)");
		Matcher m1;
		int bytes;
		for (int i = 0; i<out.size(); i++){
//			m1 = p.matcher(a);
//			if (m1.find() == true)
//			{
//				bytes = Code.bytesToAddCode.get(m1.group());
//			}
//			else{
//				bytes = 1;
//			}
//			System.out.println(bytecount + ": " + a);
//			bytecount +=bytes;
			String sout = out.get(i);
			if(!(sout=="Skip")){
				System.out.println(i + ": " + sout);
			}
		}
	out = new ArrayList<String>();
	}
}


