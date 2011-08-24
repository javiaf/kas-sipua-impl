package com.kurento.commons.sip.agent;
//package com.kurento.commons.sip.agent;
//
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//
//import javaxt.sip.address.Address;
//
//import com.kurento.commons.sip.SipUser;
//import com.kurento.commons.sip.SipUserListener;
//import com.kurento.commons.sip.exception.ServerInternalErrorException;
//
//public class SipUserFactory {
//	
//	// User List
//	private static List<SipUserRegister> users = new CopyOnWriteArrayList<SipUserRegister>();
//	
//	
//	///////////////////////////
//	//
//	// USER MANAGER
//	//
//	///////////////////////////	
//	
//	public static SipUser createUser(Address userAddress, SipUserListener handler) throws ServerInternalErrorException {
//		for (SipUserRegister u: usegetFromPartyrs){
//			if (u.compare(userAddress)) {
//				return u;
//			}
//		}
//		SipUserRegister user = new SipUserRegister(handler);
//		user.setAddress(userAddress);
//		users.add(user);
//		return user;
//	}
//	
//	public static void removeUser (SipUser user) throws ServerInternalErrorException{
//		// No concurrent problems du to CopyOnWriteArrayList
//		for (SipUserRegister u: users){
//			if (u.compare(user)) {
//				u.setExpires(0);
//				UaFactory.register(u);
//				users.remove(u);
//				u.cancelSchedule();
//			}
//		}	
//	}
//	protected static SipUserRegister getUserRegister(Address sipUri) {
//		for (SipUserRegister u: users){
//			if (u.compare(sipUri)) {
//				return u;			}
//		}
//		return null;
//	}
//
//
//
//}
